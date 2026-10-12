package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.common.BusinessTime;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.product.StockAdjustment;
import it.giovannidefilippo.gestionale.product.CostedStockReceipt;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<StockMovementResponse> recentForProduct(String productCode, int limit) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("Il codice prodotto e obbligatorio.");
        }
        return repository.findAll(
                        specification(null, null, productCode),
                        PageRequests.of(0, limit, Sort.by("timestamp").descending())
                )
                .map(StockMovementResponse::from)
                .getContent();
    }

    public List<ProductResponse> lowStockProducts() {
        return productService.findLowStockProducts(LOW_STOCK_THRESHOLD);
    }

    public InventoryReconciliationReport reconciliation() {
        List<ProductResponse> products = productService.findAll();
        List<StockMovement> movements = repository.findAllByOrderByTimestampAscIdAsc();
        Map<Long, List<StockMovement>> movementsByProduct = movements.stream()
                .filter(movement -> movement.getProductId() != null)
                .collect(Collectors.groupingBy(StockMovement::getProductId));
        List<InventoryReconciliationItem> items = products.stream()
                .map(product -> reconcile(product, movementsByProduct.getOrDefault(product.id(), List.of())))
                .toList();
        long balanced = items.stream().filter(item -> item.status() == InventoryReconciliationStatus.BALANCED).count();
        long orphanedLegacy = movements.stream()
                .filter(movement -> !movement.isAuthoritative() && movement.getProductId() == null)
                .count();
        return new InventoryReconciliationReport(
                BusinessTime.utcOffset(timeProvider.localDateTime()),
                items.size(),
                balanced,
                items.size() - balanced,
                orphanedLegacy,
                items
        );
    }

    @Transactional
    public StockMovementResponse initialBalance(InitialStockRequest request, AuthenticatedUser actor) {
        return initialBalance(request.productCode(), request.quantity(), request.reason(), actor.username(), actor.roleLabel());
    }

    @Transactional
    public StockMovementResponse initialBalance(String productCode, int quantity, String reason, String actor, String role) {
        validateReason(reason, "Inserisci una causale per il saldo iniziale.");
        ProductResponse product = productService.findByCode(productCode);
        if (repository.hasInitialBalance(product.id())) {
            throw new ResourceConflictException("Il saldo iniziale del prodotto è già stato registrato.");
        }
        StockAdjustment adjustment = productService.initializeStock(productCode, quantity);
        StockMovement movement = saveMovement(new StockMovement(
                adjustment.productId(), actor, role, adjustment.productCode(), adjustment.productName(),
                StockMovementType.INITIAL_BALANCE, quantity, 0, quantity, reason,
                timeProvider.localDateTime(), StockMovementOrigin.MANUAL_INITIAL_BALANCE, true
        ));
        auditService.record(actor, role, "STOCK_INITIAL_BALANCE", adjustment.productCode(), reason + " - saldo iniziale " + quantity, AuditCategory.INVENTORY, AuditSeverity.INFO, "STOCK_MOVEMENT");
        return StockMovementResponse.from(movement);
    }

    @Transactional
    public StockMovementResponse adjust(InventoryAdjustmentRequest request, AuthenticatedUser actor) {
        throw new ResourceConflictException("Le rettifiche inventariali devono essere originate da una sessione di conteggio approvata.");
    }

    @Transactional
    public StockMovementResponse adjust(String productCode, int delta, String reason, String actor, String role) {
        throw new ResourceConflictException("Le rettifiche inventariali devono essere originate da una sessione di conteggio approvata.");
    }

    @Transactional
    PhysicalInventoryAdjustmentResult applyPhysicalInventoryAdjustment(PhysicalInventoryAdjustmentCommand command) {
        if (command == null || command.sessionId() == null || command.itemId() == null) {
            throw new IllegalArgumentException("La sessione e la riga di inventario sono obbligatorie.");
        }
        if (repository.existsByPhysicalInventoryItemId(command.itemId())) {
            throw new ResourceConflictException("La riga di inventario è già stata applicata al ledger.");
        }
        StockAdjustment adjustment = productService.adjustStock(command.productId(), command.differenceQuantity());
        int reservedQuantity = productService.lockInventorySnapshot(command.productId()).reservedQuantity();
        if (command.differenceQuantity() == 0) {
            return new PhysicalInventoryAdjustmentResult(
                    adjustment.previousQuantity(), adjustment.newQuantity(), reservedQuantity, null
            );
        }
        StockMovementType type = command.differenceQuantity() > 0
                ? StockMovementType.PHYSICAL_INVENTORY_INCREASE
                : StockMovementType.PHYSICAL_INVENTORY_DECREASE;
        String reason = "Inventario " + command.sessionCode() + " - " + command.approvalReason().trim();
        StockMovement movement = saveMovement(new StockMovement(
                adjustment.productId(), command.actor(), command.role(), command.productCode(), command.productName(),
                type, command.differenceQuantity(), adjustment.previousQuantity(), adjustment.newQuantity(), reason,
                timeProvider.localDateTime(), command.sessionId(), command.itemId()
        ));
        auditService.record(
                command.actor(), command.role(), "STOCK_PHYSICAL_INVENTORY_ADJUSTMENT", command.productCode(),
                reason + " - differenza " + command.differenceQuantity() + " - giacenza "
                        + adjustment.previousQuantity() + " -> " + adjustment.newQuantity(),
                AuditCategory.INVENTORY, AuditSeverity.WARNING, "STOCK_MOVEMENT"
        );
        return new PhysicalInventoryAdjustmentResult(
                adjustment.previousQuantity(), adjustment.newQuantity(), reservedQuantity, movement.getId()
        );
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
        validateReason(reason, "Inserisci una causale per il movimento.");
        if (type != StockMovementType.LOAD && type != StockMovementType.UNLOAD) {
            throw new IllegalArgumentException("Il tipo di movimento manuale deve essere carico o scarico.");
        }
        requireInitializedProduct(productCode);
        int delta = type == StockMovementType.LOAD ? quantity : -quantity;
        StockAdjustment adjustment = productService.adjustStock(productCode, delta);
        StockMovement movement = saveMovement(new StockMovement(
                adjustment.productId(), actor, role, adjustment.productCode(), adjustment.productName(), type,
                delta, adjustment.previousQuantity(), adjustment.newQuantity(), reason,
                timeProvider.localDateTime(), StockMovementOrigin.MANUAL_MOVEMENT, false
        ));
        auditService.record(actor, role, "STOCK_" + type.name(), adjustment.productCode(), reason + " - quantita " + quantity + " - giacenza " + adjustment.previousQuantity() + " -> " + adjustment.newQuantity(), AuditCategory.INVENTORY, type == StockMovementType.UNLOAD ? AuditSeverity.WARNING : AuditSeverity.INFO, "STOCK_MOVEMENT");
        return StockMovementResponse.from(movement);
    }

    @Transactional
    public StockMovementResponse fulfillReserved(String productCode, int quantity, String reason, String actor, String role) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantità del movimento deve essere maggiore di zero.");
        }
        validateReason(reason, "Inserisci una causale per il movimento.");
        requireInitializedProduct(productCode);
        StockAdjustment adjustment = productService.fulfillReservedStock(productCode, quantity);
        StockMovement movement = saveMovement(new StockMovement(
                adjustment.productId(), actor, role, adjustment.productCode(), adjustment.productName(),
                StockMovementType.UNLOAD, -quantity, adjustment.previousQuantity(), adjustment.newQuantity(), reason,
                timeProvider.localDateTime(), StockMovementOrigin.ORDER_FULFILLMENT, false
        ));
        auditService.record(actor, role, "STOCK_UNLOAD", adjustment.productCode(), reason + " - quantita " + quantity + " - giacenza " + adjustment.previousQuantity() + " -> " + adjustment.newQuantity(), AuditCategory.INVENTORY, AuditSeverity.WARNING, "STOCK_MOVEMENT");
        return StockMovementResponse.from(movement);
    }

    @Transactional
    public StockMovementResponse registerReturn(String productCode, int quantity, String reason, String actor, String role) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantita resa deve essere maggiore di zero.");
        }
        validateReason(reason, "Inserisci una causale per il reso.");
        requireInitializedProduct(productCode);
        StockAdjustment adjustment = productService.adjustStock(productCode, quantity);
        StockMovement movement = saveMovement(new StockMovement(
                adjustment.productId(), actor, role, adjustment.productCode(), adjustment.productName(),
                StockMovementType.RETURN, quantity, adjustment.previousQuantity(), adjustment.newQuantity(), reason,
                timeProvider.localDateTime(), StockMovementOrigin.CUSTOMER_RETURN, false
        ));
        auditService.record(actor, role, "STOCK_RETURN", adjustment.productCode(), reason + " - quantita " + quantity + " - giacenza " + adjustment.previousQuantity() + " -> " + adjustment.newQuantity(), AuditCategory.INVENTORY, AuditSeverity.INFO, "STOCK_MOVEMENT");
        return StockMovementResponse.from(movement);
    }

    @Transactional
    public StockMovementResponse receivePurchase(PurchaseReceiptPostingCommand command) {
        if (command == null || command.supplierOrderReceiptItemId() == null) {
            throw new IllegalArgumentException("La sorgente della ricezione fornitore e obbligatoria.");
        }
        if (repository.existsBySupplierOrderReceiptItemId(command.supplierOrderReceiptItemId())) {
            throw new ResourceConflictException("La riga di ricezione e gia stata registrata nel ledger di magazzino.");
        }
        if (command.quantity() <= 0) {
            throw new IllegalArgumentException("La quantita ricevuta deve essere maggiore di zero.");
        }
        validateReason(command.reason(), "Inserisci una causale per la ricezione fornitore.");
        CostedStockReceipt receipt = productService.receivePurchaseStock(command.productCode(), command.quantity(), command.unitCost());
        boolean hasBaseline = repository.hasInitialBalance(receipt.productId());
        if (!hasBaseline && receipt.previousQuantity() != 0) {
            throw new ResourceConflictException("Registra o riconcilia il saldo iniziale prima della ricezione fornitore.");
        }
        boolean baseline = !hasBaseline;
        StockMovement movement = saveMovement(new StockMovement(
                receipt.productId(), command.actor(), command.role(), receipt.productCode(), receipt.productName(),
                command.quantity(), receipt.previousQuantity(), receipt.newQuantity(),
                "Ricezione " + command.supplierReceiptCode() + " da ordine " + command.supplierOrderCode() + " - " + command.reason().trim(),
                command.receivedAt(), baseline, command.supplierOrderId(), command.supplierOrderReceiptId(),
                command.supplierOrderReceiptItemId(), receipt.unitCost(), receipt.totalCost(), receipt.previousAverageCost(),
                receipt.newAverageCost(), receipt.previousCostedQuantity(), receipt.newCostedQuantity()
        ));
        auditService.record(
                command.actor(), command.role(), "STOCK_PURCHASE_RECEIPT", receipt.productCode(),
                "Ricezione " + command.supplierReceiptCode() + " - quantita " + command.quantity()
                        + " - costo unitario " + receipt.unitCost() + " - costo medio " + receipt.newAverageCost()
                        + " - giacenza " + receipt.previousQuantity() + " -> " + receipt.newQuantity(),
                AuditCategory.INVENTORY, AuditSeverity.INFO, "STOCK_MOVEMENT"
        );
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

    private ProductResponse requireInitializedProduct(String productCode) {
        ProductResponse product = productService.findByCode(productCode);
        if (!repository.hasInitialBalance(product.id())) {
            throw new ResourceConflictException("Registra il saldo iniziale del prodotto prima di movimentare la giacenza.");
        }
        return product;
    }

    private StockMovement saveMovement(StockMovement movement) {
        try {
            return repository.saveAndFlush(movement);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("Il movimento di magazzino non è compatibile con lo stato corrente del ledger.");
        }
    }

    private InventoryReconciliationItem reconcile(ProductResponse product, List<StockMovement> movements) {
        List<StockMovement> authoritative = movements.stream().filter(StockMovement::isAuthoritative).toList();
        long legacyMovements = movements.size() - authoritative.size();
        boolean hasBaseline = authoritative.stream().anyMatch(StockMovement::isBaseline);
        boolean unverifiedBaseline = authoritative.stream()
                .anyMatch(movement -> movement.isBaseline() && movement.getOrigin() == StockMovementOrigin.MIGRATION_BASELINE);
        int ledgerQuantity = authoritative.stream().mapToInt(StockMovement::getDeltaQuantity).sum();
        boolean chainValid = isChainValid(authoritative);
        InventoryReconciliationStatus status;
        if (!hasBaseline) {
            status = InventoryReconciliationStatus.MISSING_INITIAL_BALANCE;
        } else if (product.quantity() != ledgerQuantity) {
            status = InventoryReconciliationStatus.LEDGER_DRIFT;
        } else if (!chainValid) {
            status = InventoryReconciliationStatus.CHAIN_BROKEN;
        } else if (unverifiedBaseline) {
            status = InventoryReconciliationStatus.UNVERIFIED_INITIAL_BALANCE;
        } else {
            status = InventoryReconciliationStatus.BALANCED;
        }
        return new InventoryReconciliationItem(
                product.id(), product.code(), product.name(), product.quantity(), ledgerQuantity,
                product.reservedQuantity(), authoritative.size(), legacyMovements, status, status.getLabel()
        );
    }

    private static boolean isChainValid(List<StockMovement> movements) {
        if (movements.isEmpty() || !movements.get(0).isBaseline()) {
            return false;
        }
        int expected = 0;
        for (StockMovement movement : movements) {
            if (movement.getPreviousQuantity() != expected || movement.getNewQuantity() != expected + movement.getDeltaQuantity()) {
                return false;
            }
            expected = movement.getNewQuantity();
        }
        return true;
    }

    private static void validateReason(String reason, String message) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
