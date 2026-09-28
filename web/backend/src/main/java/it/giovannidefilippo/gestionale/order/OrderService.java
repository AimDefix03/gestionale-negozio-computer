package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.BusinessCodeGenerator;
import it.giovannidefilippo.gestionale.common.ForbiddenException;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.inventory.StockMovementType;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.product.Product;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import it.giovannidefilippo.gestionale.user.UserRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final CustomerOrderRepository repository;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final BusinessCodeGenerator codeGenerator;
    private final BusinessPartnerService partnerService;
    private final TimeProvider timeProvider;

    OrderService(CustomerOrderRepository repository, ProductService productService, InventoryService inventoryService, AuditService auditService, BusinessCodeGenerator codeGenerator, BusinessPartnerService partnerService, TimeProvider timeProvider) {
        this.repository = repository;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
        this.partnerService = partnerService;
        this.timeProvider = timeProvider;
    }

    public List<OrderResponse> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(CustomerOrder::getTimestamp).reversed())
                .map(OrderResponse::from)
                .toList();
    }

    public List<OrderResponse> findByCustomerAccount(long customerAccountId) {
        return repository.findByCustomerAccountId(customerAccountId).stream()
                .sorted(Comparator.comparing(CustomerOrder::getTimestamp).reversed())
                .map(OrderResponse::from)
                .toList();
    }

    public PageResponse<OrderResponse> search(String q, String customer, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, customer, null), PageRequests.of(page, size, Sort.by("timestamp").descending()))
                .map(OrderResponse::from));
    }

    public PageResponse<OrderResponse> search(String q, String customer, int page, int size, AuthenticatedUser actor) {
        return PageResponse.from(repository.findAll(specification(q, customer, null), PageRequests.of(page, size, Sort.by("timestamp").descending()))
                .map(order -> response(order, actor)));
    }

    public PageResponse<OrderResponse> searchForAccount(String q, long customerAccountId, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, null, customerAccountId), PageRequests.of(page, size, Sort.by("timestamp").descending()))
                .map(OrderResponse::from));
    }

    public PageResponse<OrderResponse> searchForAccount(String q, long customerAccountId, int page, int size, AuthenticatedUser actor) {
        return PageResponse.from(repository.findAll(specification(q, null, customerAccountId), PageRequests.of(page, size, Sort.by("timestamp").descending()))
                .map(order -> response(order, actor)));
    }

    public OrderDashboardSummary dashboardSummary() {
        BigDecimal grossCollected = money(repository.sumGrossCollected());
        BigDecimal refunded = money(repository.sumRefunded());
        return new OrderDashboardSummary(
                repository.count(),
                repository.countByStatus(OrderStatus.DRAFT),
                repository.countByStatus(OrderStatus.CONFIRMED),
                repository.countByStatus(OrderStatus.FULFILLED),
                repository.countByStatus(OrderStatus.CANCELED),
                money(repository.sumTotalByStatus(OrderStatus.DRAFT)),
                money(repository.sumTotalByStatus(OrderStatus.CONFIRMED)),
                money(repository.sumTotalByStatus(OrderStatus.FULFILLED)),
                grossCollected,
                refunded,
                money(grossCollected.subtract(refunded))
        );
    }

    public List<OrderResponse> recentForDashboard(AuthenticatedUser actor, int limit) {
        if (actor.role() == UserRole.CUSTOMER) {
            return searchForAccount(null, requireAccountId(actor), 0, limit, actor).content();
        }
        return search(null, null, 0, limit, actor).content();
    }

    public List<ProductOrderHistoryResponse> recentByProduct(String productCode, int limit) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("Il codice prodotto e obbligatorio.");
        }
        Specification<CustomerOrder> productHistory = (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.join("items").get("productCode")),
                    productCode.trim().toLowerCase(Locale.ROOT)
            );
        };
        return repository.findAll(productHistory, PageRequests.of(0, limit, Sort.by("timestamp").descending()))
                .stream()
                .map(order -> ProductOrderHistoryResponse.from(order, productCode))
                .toList();
    }

    public CustomerOrderDashboardSummary customerDashboardSummary(AuthenticatedUser actor, int limit) {
        if (actor.role() != UserRole.CUSTOMER) {
            throw new ForbiddenException("La dashboard cliente e disponibile soltanto per account cliente.");
        }
        long accountId = requireAccountId(actor);
        List<CustomerOrderSummaryResponse> recentOrders = repository.findAll(
                        specification(null, null, accountId),
                        PageRequests.of(0, limit, Sort.by("timestamp").descending())
                )
                .map(CustomerOrderSummaryResponse::from)
                .getContent();
        return new CustomerOrderDashboardSummary(
                repository.countByCustomerAccountId(accountId),
                repository.countByCustomerAccountIdAndStatus(accountId, OrderStatus.DRAFT),
                repository.countByCustomerAccountIdAndStatus(accountId, OrderStatus.CONFIRMED),
                repository.countByCustomerAccountIdAndStatus(accountId, OrderStatus.FULFILLED),
                repository.countByCustomerAccountIdAndStatus(accountId, OrderStatus.CANCELED),
                recentOrders
        );
    }

    public CustomerOrder requireOrder(String code) {
        return repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Ordine non trovato."));
    }

    public OrderResponse findByCode(String code, AuthenticatedUser actor) {
        CustomerOrder order = requireOrder(code);
        requireCustomerOwnership(order, actor);
        return response(order, actor);
    }

    @Transactional
    public OrderResponse create(OrderRequests.CreateOrderRequest request, String customer, AuthenticatedUser actor) {
        Map<String, Integer> quantities = request.items().stream()
                .collect(Collectors.toMap(
                        item -> item.productCode().trim(),
                        OrderRequests.CreateOrderItemRequest::quantity,
                        Integer::sum
                ));

        List<OrderItem> items = quantities.entrySet().stream()
                .map(entry -> toOrderItem(entry.getKey(), entry.getValue()))
                .toList();
        String orderCode = generateCode();
        CustomerData customerData = resolveCustomer(request, customer, actor);

        CustomerOrder order = repository.save(new CustomerOrder(
                orderCode,
                customerData.name(),
                customerData.code(),
                customerData.accountId(),
                customerData.partnerId(),
                customerData.type(),
                request.paymentMethod(),
                items,
                timeProvider.localDateTime()
        ));
        String action = actor.role() == UserRole.CUSTOMER ? "CREATE_ORDER" : "CREATE_ASSISTED_ORDER";
        auditService.record(actor.username(), actor.roleLabel(), action, order.getCode(), "Bozza ordine creata - cliente " + order.getCustomer() + " - modalita " + order.getCustomerType().getLabel() + " - partner ID " + (order.getPartnerId() == null ? "-" : order.getPartnerId()) + " - righe " + items.size() + " - totale " + order.getTotal() + " - pagamento " + order.getPayment().getMethod().getLabel(), AuditCategory.ORDER, AuditSeverity.INFO, "CUSTOMER_ORDER");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse confirm(String code, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        for (OrderItem item : order.getItems()) {
            productService.reserveStock(item.getProductCode(), item.getQuantity());
        }
        order.confirm(timeProvider.localDateTime());
        auditService.record(actor.username(), actor.roleLabel(), "CONFIRM_ORDER", order.getCode(), "Ordine confermato per " + order.getCustomer() + " - stock riservato", AuditCategory.ORDER, AuditSeverity.WARNING, "CUSTOMER_ORDER");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse fulfill(String code, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        order.fulfill(timeProvider.localDateTime());
        for (OrderItem item : order.getItems()) {
            inventoryService.fulfillReserved(item.getProductCode(), item.getQuantity(), "Evasione ordine " + order.getCode(), actor.username(), actor.roleLabel());
        }
        auditService.record(actor.username(), actor.roleLabel(), "FULFILL_ORDER", order.getCode(), "Ordine evaso per " + order.getCustomer(), AuditCategory.ORDER, AuditSeverity.INFO, "CUSTOMER_ORDER");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse cancel(String code, OrderOperationRequests.CancellationRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        if (actor.role() == UserRole.CUSTOMER && order.getStatus() != OrderStatus.DRAFT) {
            throw new ForbiddenException("Puoi annullare solo ordini in bozza.");
        }
        String transactionCode = order.requiresCancellationReversal()
                ? codeGenerator.nextPaymentTransactionCode()
                : null;
        OrderCancellationResult cancellation = order.cancel(
                transactionCode,
                request.reference(),
                request.reason(),
                timeProvider.localDateTime(),
                actor.username(),
                actor.roleLabel()
        );
        if (cancellation.previousStatus() == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems().stream().sorted(Comparator.comparing(OrderItem::getProductCode)).toList()) {
                productService.releaseReservedStock(item.getProductCode(), item.getQuantity());
            }
        }
        repository.saveAndFlush(order);
        PaymentTransaction reversal = cancellation.reversal();
        String reversalDetail = reversal == null
                ? "nessuno storno necessario"
                : "storno " + reversal.getCode() + " di " + reversal.getAmount() + " EUR - riferimento " + reversal.getReference();
        auditService.record(
                actor.username(),
                actor.roleLabel(),
                "CANCEL_ORDER",
                order.getCode(),
                "Ordine annullato per " + order.getCustomer()
                        + " - stato precedente " + cancellation.previousStatus().getLabel()
                        + " - " + reversalDetail
                        + " - motivazione " + request.reason().trim(),
                AuditCategory.ORDER,
                reversal == null ? AuditSeverity.WARNING : AuditSeverity.CRITICAL,
                "CUSTOMER_ORDER"
        );
        return response(order, actor);
    }

    @Transactional
    public OrderResponse recordReceipt(String code, OrderOperationRequests.ReceiptRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.FULFILLED) {
            throw new IllegalStateException("Puoi registrare un incasso solo su un ordine confermato o evaso.");
        }
        PaymentTransaction transaction = order.getPayment().recordReceipt(
                codeGenerator.nextPaymentTransactionCode(),
                request.amount(),
                request.reference(),
                request.reason(),
                timeProvider.localDateTime(),
                actor.username(),
                actor.roleLabel()
        );
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "RECORD_PAYMENT", order.getCode(), "Incasso " + transaction.getCode() + " di " + transaction.getAmount() + " EUR - residuo " + order.getPayment().getOutstandingAmount(), AuditCategory.ORDER, AuditSeverity.WARNING, "PAYMENT_TRANSACTION");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse reconcilePayment(String code, OrderOperationRequests.PaymentReconciliationRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        order.getPayment().reconcile(
                codeGenerator.nextPaymentTransactionCode(),
                request.verifiedPaidAmount(),
                request.reference(),
                request.reason(),
                timeProvider.localDateTime(),
                actor.username(),
                actor.roleLabel()
        );
        repository.saveAndFlush(order);
        auditService.record(
                actor.username(),
                actor.roleLabel(),
                "RECONCILE_HISTORICAL_PAYMENT",
                order.getCode(),
                "Pagamento storico riconciliato con importo verificato " + request.verifiedPaidAmount() + " EUR - " + request.reason(),
                AuditCategory.ORDER,
                AuditSeverity.CRITICAL,
                "ORDER_PAYMENT"
        );
        return response(order, actor);
    }

    @Transactional
    public OrderResponse requestReturn(String code, OrderOperationRequests.ReturnRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        if (order.getStatus() != OrderStatus.FULFILLED) {
            throw new IllegalStateException("Puoi richiedere un reso solo per un ordine evaso.");
        }
        Map<String, Integer> quantities = request.items().stream().collect(Collectors.toMap(
                item -> item.productCode().trim(),
                OrderOperationRequests.ReturnItemRequest::quantity,
                Integer::sum
        ));
        List<OrderReturnItem> items = quantities.entrySet().stream()
                .map(entry -> toReturnItem(order, entry.getKey(), entry.getValue()))
                .toList();
        OrderReturn orderReturn = new OrderReturn(codeGenerator.nextOrderReturnCode(), request.reason(), items, timeProvider.localDateTime(), actor.username(), actor.roleLabel());
        order.addReturn(orderReturn);
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "REQUEST_RETURN", orderReturn.getCode(), "Reso richiesto per ordine " + order.getCode() + " - valore " + orderReturn.getTotalAmount(), AuditCategory.ORDER, AuditSeverity.WARNING, "ORDER_RETURN");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse approveReturn(String orderCode, String returnCode, OrderOperationRequests.ReturnReviewRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(orderCode, actor);
        OrderReturn orderReturn = requireReturn(order, returnCode);
        orderReturn.approve(actor.username(), request.note(), timeProvider.localDateTime());
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "APPROVE_RETURN", returnCode, "Reso approvato per ordine " + order.getCode(), AuditCategory.ORDER, AuditSeverity.WARNING, "ORDER_RETURN");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse rejectReturn(String orderCode, String returnCode, OrderOperationRequests.ReturnRejectionRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(orderCode, actor);
        OrderReturn orderReturn = requireReturn(order, returnCode);
        orderReturn.reject(actor.username(), request.note(), timeProvider.localDateTime());
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "REJECT_RETURN", returnCode, "Reso rifiutato per ordine " + order.getCode() + " - " + request.note(), AuditCategory.ORDER, AuditSeverity.WARNING, "ORDER_RETURN");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse receiveReturn(String orderCode, String returnCode, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(orderCode, actor);
        OrderReturn orderReturn = requireReturn(order, returnCode);
        for (OrderReturnItem item : orderReturn.getItems()) {
            inventoryService.registerReturn(item.getProductCode(), item.getQuantity(), "Ricezione reso " + returnCode + " per ordine " + order.getCode(), actor.username(), actor.roleLabel());
        }
        orderReturn.receive(actor.username(), timeProvider.localDateTime());
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "RECEIVE_RETURN", returnCode, "Reso ricevuto e giacenze reintegrate per ordine " + order.getCode(), AuditCategory.ORDER, AuditSeverity.WARNING, "ORDER_RETURN");
        return response(order, actor);
    }

    @Transactional
    public OrderResponse refundReturn(String orderCode, String returnCode, OrderOperationRequests.ReturnRefundRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(orderCode, actor);
        OrderReturn orderReturn = requireReturn(order, returnCode);
        LocalDateTime changedAt = timeProvider.localDateTime();
        PaymentTransaction transaction = order.getPayment().recordRefund(
                codeGenerator.nextPaymentTransactionCode(),
                orderReturn,
                request.amount(),
                request.reference(),
                request.reason(),
                changedAt,
                actor.username(),
                actor.roleLabel()
        );
        orderReturn.synchronizeRefundedAmount(order.getPayment().refundedForReturn(orderReturn), changedAt);
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "REFUND_RETURN", returnCode, "Rimborso " + transaction.getCode() + " di " + transaction.getAmount() + " EUR per ordine " + order.getCode(), AuditCategory.ORDER, AuditSeverity.CRITICAL, "PAYMENT_TRANSACTION");
        return response(order, actor);
    }

    private OrderResponse response(CustomerOrder order, AuthenticatedUser actor) {
        return OrderResponse.from(order, capabilities(order, actor));
    }

    private OrderCapabilities capabilities(CustomerOrder order, AuthenticatedUser actor) {
        boolean owned = actor.role() != UserRole.CUSTOMER
                || actor.accountId() != null && order.isOwnedBy(actor.accountId());
        boolean canCancelState = order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.CONFIRMED;
        boolean customerCanCancel = actor.role() != UserRole.CUSTOMER || order.getStatus() == OrderStatus.DRAFT;
        boolean canRequestReturn = owned
                && actor.hasPermission(UserPermission.REQUEST_RETURNS)
                && order.getStatus() == OrderStatus.FULFILLED
                && order.getItems().stream().anyMatch(item -> item.getQuantity() > order.returnedOrReservedQuantity(item.getProductCode()));
        boolean paymentAvailable = !order.getPayment().isReconciliationRequired()
                && order.getPayment().getOutstandingAmount().signum() > 0;
        List<OrderCapabilities.ReturnCapabilities> returnCapabilities = order.getReturns().stream()
                .map(orderReturn -> new OrderCapabilities.ReturnCapabilities(
                        orderReturn.getCode(),
                        actor.hasPermission(UserPermission.MANAGE_RETURNS) && orderReturn.getStatus() == OrderReturnStatus.REQUESTED,
                        actor.hasPermission(UserPermission.MANAGE_RETURNS) && orderReturn.getStatus() == OrderReturnStatus.REQUESTED,
                        actor.hasPermission(UserPermission.MANAGE_RETURNS) && orderReturn.getStatus() == OrderReturnStatus.APPROVED,
                        actor.hasPermission(UserPermission.MANAGE_RETURNS)
                                && actor.hasPermission(UserPermission.REFUND_PAYMENTS)
                                && (orderReturn.getStatus() == OrderReturnStatus.RECEIVED || orderReturn.getStatus() == OrderReturnStatus.PARTIALLY_REFUNDED)
                                && orderReturn.getRefundableAmount().signum() > 0
                                && !order.getPayment().isReconciliationRequired()
                                && order.getPayment().getRefundableAmount().signum() > 0
                ))
                .toList();
        return new OrderCapabilities(
                owned && actor.hasPermission(UserPermission.CONFIRM_ORDERS) && order.getStatus() == OrderStatus.DRAFT,
                owned && actor.hasPermission(UserPermission.FULFILL_ORDERS) && order.getStatus() == OrderStatus.CONFIRMED,
                owned && actor.hasPermission(UserPermission.CANCEL_ORDERS) && canCancelState && customerCanCancel,
                owned && actor.hasPermission(UserPermission.RECORD_PAYMENTS)
                        && (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.FULFILLED)
                        && paymentAvailable,
                canRequestReturn,
                returnCapabilities
        );
    }

    private OrderItem toOrderItem(String productCode, int quantity) {
        Product product = productService.requireProduct(productCode);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantità richiesta non valida.");
        }
        if (product.isDiscontinued()) {
            throw new IllegalArgumentException("Il prodotto " + product.getCode() + " e disattivato e non puo essere acquistato.");
        }
        if (product.getAvailableQuantity() < quantity) {
            throw new IllegalArgumentException("Scorte insufficienti per il prodotto " + product.getCode() + ".");
        }
        return new OrderItem(product.getCode(), product.getName(), product.getDescription(), quantity, product.getDiscountedPrice());
    }

    private OrderReturnItem toReturnItem(CustomerOrder order, String productCode, int quantity) {
        OrderItem orderedItem = order.getItems().stream()
                .filter(item -> item.getProductCode().equalsIgnoreCase(productCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Il prodotto " + productCode + " non appartiene all'ordine."));
        int availableToReturn = orderedItem.getQuantity() - order.returnedOrReservedQuantity(orderedItem.getProductCode());
        if (quantity <= 0 || quantity > availableToReturn) {
            throw new IllegalArgumentException("Quantita non restituibile per il prodotto " + orderedItem.getProductCode() + ". Disponibile: " + availableToReturn + ".");
        }
        return new OrderReturnItem(orderedItem.getProductCode(), orderedItem.getProductName(), quantity, orderedItem.getUnitPrice());
    }

    private OrderReturn requireReturn(CustomerOrder order, String returnCode) {
        return order.getReturns().stream()
                .filter(orderReturn -> orderReturn.getCode().equalsIgnoreCase(returnCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reso non trovato per l'ordine indicato."));
    }

    private String generateCode() {
        String code;
        do {
            code = codeGenerator.nextOrderCode();
        } while (repository.existsByCodeIgnoreCase(code));
        return code;
    }

    private CustomerData resolveCustomer(OrderRequests.CreateOrderRequest request, String fallbackCustomer, AuthenticatedUser actor) {
        if (actor.role() == UserRole.CUSTOMER) {
            if ((request.customerType() != null && request.customerType() != OrderCustomerType.SELF_SERVICE)
                    || request.customerPartnerId() != null
                    || hasText(request.customerCode())
                    || hasText(request.walkInCustomerName())) {
                throw new ForbiddenException("Un cliente puo creare ordini soltanto per il proprio account.");
            }
            long accountId = requireAccountId(actor);
            BusinessPartnerResponse linkedPartner = partnerService.findActiveCustomerByLinkedAccount(accountId);
            if (linkedPartner != null) {
                return new CustomerData(linkedPartner.displayName(), linkedPartner.code(), accountId, linkedPartner.id(), OrderCustomerType.SELF_SERVICE);
            }
            return new CustomerData(actor.username(), "", accountId, null, OrderCustomerType.SELF_SERVICE);
        }

        if (request.customerType() == OrderCustomerType.REGISTERED) {
            if (request.customerPartnerId() == null || hasText(request.walkInCustomerName()) || hasText(request.customer()) || hasText(request.customerCode())) {
                throw new IllegalArgumentException("Seleziona una sola anagrafica cliente attiva.");
            }
            BusinessPartnerResponse partner = partnerService.requireActiveCustomer(request.customerPartnerId());
            return new CustomerData(partner.displayName(), partner.code(), partner.linkedAccountId(), partner.id(), OrderCustomerType.REGISTERED);
        }
        if (request.customerType() == OrderCustomerType.WALK_IN) {
            if (request.customerPartnerId() != null || !hasText(request.walkInCustomerName()) || hasText(request.customer()) || hasText(request.customerCode())) {
                throw new IllegalArgumentException("Inserisci il nominativo del cliente occasionale.");
            }
            return new CustomerData(request.walkInCustomerName().trim(), "", null, null, OrderCustomerType.WALK_IN);
        }
        if (request.customerType() == OrderCustomerType.SELF_SERVICE) {
            throw new ForbiddenException("La modalita self-service e riservata agli account cliente.");
        }
        if (request.customerType() == OrderCustomerType.LEGACY_UNRESOLVED) {
            throw new IllegalArgumentException("La classificazione storica non e selezionabile per nuovi ordini.");
        }

        if (hasText(request.customerCode())) {
            BusinessPartnerResponse partner = partnerService.requireActiveCustomer(request.customerCode());
            return new CustomerData(partner.displayName(), partner.code(), partner.linkedAccountId(), partner.id(), OrderCustomerType.REGISTERED);
        }
        if (hasText(request.customer())) {
            return new CustomerData(request.customer().trim(), "", null, null, OrderCustomerType.WALK_IN);
        }
        if (hasText(fallbackCustomer)) {
            return new CustomerData(fallbackCustomer.trim(), "", null, null, OrderCustomerType.WALK_IN);
        }
        throw new IllegalArgumentException("Seleziona un cliente censito oppure indica un cliente occasionale.");
    }

    private CustomerOrder requireOrderForTransition(String code, AuthenticatedUser actor) {
        CustomerOrder order = repository.findByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("Ordine non trovato."));
        requireCustomerOwnership(order, actor);
        return order;
    }

    private void requireCustomerOwnership(CustomerOrder order, AuthenticatedUser actor) {
        if (actor.role() == UserRole.CUSTOMER && !order.isOwnedBy(requireAccountId(actor))) {
            throw new ForbiddenException("Puoi operare solo sui tuoi ordini.");
        }
    }

    private long requireAccountId(AuthenticatedUser actor) {
        if (actor.accountId() == null) {
            throw new ForbiddenException("La sessione non contiene un'identita account stabile.");
        }
        return actor.accountId();
    }

    private Specification<CustomerOrder> specification(String q, String customer, Long customerAccountId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(q)) {
                String term = contains(q);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerCode")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customer")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("paymentMethod")), term)
                ));
            }
            if (hasText(customer)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("customer")), customer.trim().toLowerCase(Locale.ROOT)));
            }
            if (customerAccountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerAccountId"), customerAccountId));
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

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record CustomerData(String name, String code, Long accountId, Long partnerId, OrderCustomerType type) {
    }
}
