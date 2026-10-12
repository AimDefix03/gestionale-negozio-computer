package it.giovannidefilippo.gestionale.purchase;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.BusinessCodeGenerator;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.inventory.PurchaseReceiptPostingCommand;
import it.giovannidefilippo.gestionale.inventory.StockMovementResponse;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SupplierOrderService {
    private final SupplierOrderRepository repository;
    private final SupplierOrderReceiptRepository receiptRepository;
    private final BusinessPartnerService partnerService;
    private final ProductService productService;
    private final BusinessCodeGenerator codeGenerator;
    private final AuditService auditService;
    private final TimeProvider timeProvider;
    private final InventoryService inventoryService;

    SupplierOrderService(SupplierOrderRepository repository, SupplierOrderReceiptRepository receiptRepository, BusinessPartnerService partnerService, ProductService productService, BusinessCodeGenerator codeGenerator, AuditService auditService, TimeProvider timeProvider, InventoryService inventoryService) {
        this.repository = repository;
        this.receiptRepository = receiptRepository;
        this.partnerService = partnerService;
        this.productService = productService;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
        this.inventoryService = inventoryService;
    }

    public PageResponse<SupplierOrderSummaryResponse> search(String q, SupplierOrderStatus status, Long supplierId, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, status, supplierId), PageRequests.of(page, size, Sort.by("createdAt").descending()))
                .map(SupplierOrderSummaryResponse::from));
    }

    public SupplierOrderResponse findByCode(String code, AuthenticatedUser actor) {
        return response(requireOrder(code), actor);
    }

    @Transactional
    public SupplierOrderResponse create(SupplierOrderRequests.CreateRequest request, AuthenticatedUser actor) {
        BusinessPartnerResponse supplier = partnerService.requireActiveSupplier(request.supplierId());
        List<SupplierOrderItem> items = createItems(request);
        SupplierOrder order = new SupplierOrder(
                codeGenerator.nextSupplierOrderCode(), supplier.id(), supplier.code(), supplier.displayName(),
                request.expectedDeliveryDate(), request.notes(), items, timeProvider.localDateTime(), actor.username(), actor.roleLabel()
        );
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "CREATE_SUPPLIER_ORDER", order.getCode(), "Ordine fornitore creato in bozza - fornitore " + order.getSupplierCodeSnapshot() + " - righe " + order.getItems().size() + " - totale " + order.getTotal() + " EUR", AuditCategory.PURCHASE, AuditSeverity.INFO, "SUPPLIER_ORDER");
        return response(order, actor);
    }

    @Transactional
    public SupplierOrderResponse send(String code, AuthenticatedUser actor) {
        SupplierOrder order = requireOrderForTransition(code);
        order.send(timeProvider.localDateTime());
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "SEND_SUPPLIER_ORDER", order.getCode(), "Ordine inviato al fornitore " + order.getSupplierCodeSnapshot() + " - data prevista " + order.getExpectedDeliveryDate(), AuditCategory.PURCHASE, AuditSeverity.WARNING, "SUPPLIER_ORDER");
        return response(order, actor);
    }

    @Transactional
    public SupplierOrderResponse receive(String code, SupplierOrderRequests.ReceiveRequest request, AuthenticatedUser actor) {
        SupplierOrder order = requireOrderForTransition(code);
        Map<Long, SupplierReceiptLine> receivedLines = new LinkedHashMap<>();
        request.items().forEach(item -> {
            if (receivedLines.putIfAbsent(item.lineId(), new SupplierReceiptLine(item.quantity(), item.unitCost())) != null) {
                throw new IllegalArgumentException("Ogni riga ordine puo comparire una sola volta nella ricezione.");
            }
        });
        SupplierOrderReceipt receipt = order.receive(codeGenerator.nextSupplierReceiptCode(), request.reason(), receivedLines, timeProvider.localDateTime(), actor.username(), actor.roleLabel());
        receiptRepository.saveAndFlush(receipt);
        repository.saveAndFlush(order);
        for (SupplierOrderReceiptItem item : receipt.getItems()) {
            StockMovementResponse movement = inventoryService.receivePurchase(new PurchaseReceiptPostingCommand(
                    order.getId(), receipt.getId(), item.getId(), order.getCode(), receipt.getCode(),
                    item.getOrderItem().getProductCodeSnapshot(), item.getQuantity(), item.getActualUnitCost(),
                    request.reason(), receipt.getReceivedAt(), actor.username(), actor.roleLabel()
            ));
            item.markPosted(movement.id());
        }
        repository.saveAndFlush(order);
        int receivedQuantity = receipt.getItems().stream().mapToInt(SupplierOrderReceiptItem::getQuantity).sum();
        auditService.record(actor.username(), actor.roleLabel(), "RECEIVE_SUPPLIER_ORDER", order.getCode(), "Ricezione fisica " + receipt.getCode() + " - quantita " + receivedQuantity + " - righe stock " + receipt.getItems().size() + " - stato " + order.getStatus().getLabel() + " - causale " + receipt.getReason(), AuditCategory.PURCHASE, AuditSeverity.WARNING, "SUPPLIER_ORDER_RECEIPT");
        return response(order, actor);
    }

    @Transactional
    public SupplierOrderResponse cancel(String code, SupplierOrderRequests.CancelRequest request, AuthenticatedUser actor) {
        SupplierOrder order = requireOrderForTransition(code);
        int received = order.getItems().stream().mapToInt(SupplierOrderItem::getReceivedQuantity).sum();
        int residual = order.getItems().stream().mapToInt(SupplierOrderItem::remainingQuantity).sum();
        order.cancel(request.reason(), timeProvider.localDateTime(), actor.username(), actor.roleLabel());
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "CANCEL_SUPPLIER_ORDER", order.getCode(), "Ordine annullato - ricevuto preservato " + received + " - residuo chiuso " + residual + " - motivazione " + request.reason().trim(), AuditCategory.PURCHASE, AuditSeverity.WARNING, "SUPPLIER_ORDER");
        return response(order, actor);
    }

    private List<SupplierOrderItem> createItems(SupplierOrderRequests.CreateRequest request) {
        Map<String, SupplierOrderRequests.CreateItemRequest> uniqueItems = new LinkedHashMap<>();
        for (SupplierOrderRequests.CreateItemRequest item : request.items()) {
            String key = item.productCode().trim().toLowerCase(Locale.ROOT);
            if (uniqueItems.putIfAbsent(key, item) != null) {
                throw new IllegalArgumentException("Ogni prodotto puo comparire una sola volta nell'ordine fornitore.");
            }
        }
        return uniqueItems.values().stream().map(item -> {
            ProductResponse product = productService.findByCode(item.productCode());
            if (product.discontinued()) {
                throw new IllegalArgumentException("Il prodotto " + product.code() + " e disattivato e non puo essere ordinato.");
            }
            LocalDate expectedDate = item.expectedDeliveryDate() == null ? request.expectedDeliveryDate() : item.expectedDeliveryDate();
            return new SupplierOrderItem(product.id(), product.code(), product.name(), item.quantity(), item.unitPrice(), expectedDate);
        }).toList();
    }

    private SupplierOrder requireOrder(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Il codice ordine fornitore e obbligatorio.");
        return repository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Ordine fornitore non trovato."));
    }

    private SupplierOrder requireOrderForTransition(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Il codice ordine fornitore e obbligatorio.");
        return repository.findByCodeForUpdate(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Ordine fornitore non trovato."));
    }

    private SupplierOrderResponse response(SupplierOrder order, AuthenticatedUser actor) {
        boolean canManage = actor.hasPermission(UserPermission.MANAGE_PURCHASE_ORDERS);
        SupplierOrderResponse.Capabilities capabilities = canManage
                ? new SupplierOrderResponse.Capabilities(
                        order.getStatus() == SupplierOrderStatus.DRAFT,
                        (order.getStatus() == SupplierOrderStatus.SENT || order.getStatus() == SupplierOrderStatus.PARTIALLY_RECEIVED)
                                && order.getItems().stream().anyMatch(item -> item.remainingQuantity() > 0),
                        order.getStatus() == SupplierOrderStatus.DRAFT || order.getStatus() == SupplierOrderStatus.SENT || order.getStatus() == SupplierOrderStatus.PARTIALLY_RECEIVED
                )
                : SupplierOrderResponse.Capabilities.none();
        return SupplierOrderResponse.from(order, capabilities);
    }

    private Specification<SupplierOrder> specification(String q, SupplierOrderStatus status, Long supplierId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String term = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("supplierCodeSnapshot")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("supplierNameSnapshot")), term)
                ));
            }
            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));
            if (supplierId != null) predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
