package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.product.StockAdjustment;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class InventoryService {
    public static final int LOW_STOCK_THRESHOLD = 3;
    private final ProductService productService;
    private final StockMovementRepository repository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    InventoryService(ProductService productService, StockMovementRepository repository, AuditService auditService, TimeProvider timeProvider) {
        this.productService = productService;
        this.repository = repository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    public List<StockMovementResponse> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(StockMovement::getTimestamp).reversed())
                .map(StockMovementResponse::from)
                .toList();
    }

    public PageResponse<StockMovementResponse> search(String q, StockMovementType type, String productCode, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, type, productCode), PageRequests.of(page, size, Sort.by("timestamp").descending()))
                .map(StockMovementResponse::from));
    }

    public List<StockMovementResponse> recentMovements(int limit) {
        return repository.findAll(PageRequests.of(0, limit, Sort.by("timestamp").descending()))
                .map(StockMovementResponse::from)
                .getContent();
    }

    public List<ProductResponse> lowStockProducts() {
        return productService.findLowStockProducts(LOW_STOCK_THRESHOLD);
    }

    @Transactional
    public StockMovementResponse register(StockMovementRequest request, AuthenticatedUser actor) {
        return register(request.productCode(), request.type(), request.quantity(), request.reason(), actor.username(), actor.roleLabel());
    }

    @Transactional
    public StockMovementResponse register(String productCode, StockMovementType type, int quantity, String reason, String actor, String role) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantità del movimento deve essere maggiore di zero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Inserisci una causale per il movimento.");
        }
        if (type == StockMovementType.RETURN) {
            throw new IllegalArgumentException("I movimenti di reso sono generati dal workflow resi.");
        }
        int delta = type == StockMovementType.LOAD ? quantity : -quantity;
        StockAdjustment adjustment = productService.adjustStock(productCode, delta);
        StockMovement movement = repository.save(new StockMovement(actor, role, adjustment.productCode(), adjustment.productName(), type, quantity, adjustment.previousQuantity(), adjustment.newQuantity(), reason, timeProvider.localDateTime()));
        auditService.record(actor, role, "STOCK_" + type.name(), adjustment.productCode(), reason + " - quantita " + quantity + " - giacenza " + adjustment.previousQuantity() + " -> " + adjustment.newQuantity(), AuditCategory.INVENTORY, type == StockMovementType.UNLOAD ? AuditSeverity.WARNING : AuditSeverity.INFO, "STOCK_MOVEMENT");
        return StockMovementResponse.from(movement);
    }

    @Transactional
    public StockMovementResponse fulfillReserved(String productCode, int quantity, String reason, String actor, String role) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantità del movimento deve essere maggiore di zero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Inserisci una causale per il movimento.");
        }
        StockAdjustment adjustment = productService.fulfillReservedStock(productCode, quantity);
        StockMovement movement = repository.save(new StockMovement(actor, role, adjustment.productCode(), adjustment.productName(), StockMovementType.UNLOAD, quantity, adjustment.previousQuantity(), adjustment.newQuantity(), reason, timeProvider.localDateTime()));
        auditService.record(actor, role, "STOCK_UNLOAD", adjustment.productCode(), reason + " - quantita " + quantity + " - giacenza " + adjustment.previousQuantity() + " -> " + adjustment.newQuantity(), AuditCategory.INVENTORY, AuditSeverity.WARNING, "STOCK_MOVEMENT");
        return StockMovementResponse.from(movement);
    }

    @Transactional
    public StockMovementResponse registerReturn(String productCode, int quantity, String reason, String actor, String role) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantita resa deve essere maggiore di zero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Inserisci una causale per il reso.");
        }
        StockAdjustment adjustment = productService.adjustStock(productCode, quantity);
        StockMovement movement = repository.save(new StockMovement(actor, role, adjustment.productCode(), adjustment.productName(), StockMovementType.RETURN, quantity, adjustment.previousQuantity(), adjustment.newQuantity(), reason, timeProvider.localDateTime()));
        auditService.record(actor, role, "STOCK_RETURN", adjustment.productCode(), reason + " - quantita " + quantity + " - giacenza " + adjustment.previousQuantity() + " -> " + adjustment.newQuantity(), AuditCategory.INVENTORY, AuditSeverity.INFO, "STOCK_MOVEMENT");
        return StockMovementResponse.from(movement);
    }

    private Specification<StockMovement> specification(String q, StockMovementType type, String productCode) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(q)) {
                String term = contains(q);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("actor")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("role")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("productCode")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), term)
                ));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (hasText(productCode)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("productCode")), productCode.trim().toLowerCase(Locale.ROOT)));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
