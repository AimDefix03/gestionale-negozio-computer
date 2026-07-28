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
import it.giovannidefilippo.gestionale.user.UserRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    public List<OrderResponse> findByCustomer(String customer) {
        return repository.findByCustomerIgnoreCase(customer).stream()
                .sorted(Comparator.comparing(CustomerOrder::getTimestamp).reversed())
                .map(OrderResponse::from)
                .toList();
    }

    public PageResponse<OrderResponse> search(String q, String customer, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, customer), PageRequests.of(page, size, Sort.by("timestamp").descending()))
                .map(OrderResponse::from));
    }

    public long countForDashboard(AuthenticatedUser actor) {
        if (actor.role() == UserRole.CUSTOMER) {
            return repository.countByCustomerIgnoreCase(actor.username());
        }
        return repository.count();
    }

    public java.math.BigDecimal revenueForDashboard(AuthenticatedUser actor) {
        java.math.BigDecimal revenue = actor.role() == UserRole.CUSTOMER
                ? repository.sumTotalByCustomer(actor.username())
                : repository.sumTotal();
        return revenue.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public List<OrderResponse> recentForDashboard(AuthenticatedUser actor, int limit) {
        String customer = actor.role() == UserRole.CUSTOMER ? actor.username() : null;
        return search(null, customer, 0, limit).content();
    }

    public CustomerOrder requireOrder(String code) {
        return repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Ordine non trovato."));
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

        CustomerOrder order = repository.save(new CustomerOrder(orderCode, customerData.name(), customerData.code(), request.paymentMethod(), items, timeProvider.localDateTime()));
        auditService.record(actor.username(), actor.roleLabel(), "CREATE_ORDER", order.getCode(), "Bozza ordine creata per " + order.getCustomer() + " - righe " + items.size() + " - totale " + order.getTotal() + " - pagamento " + order.getPayment().getMethod().getLabel(), AuditCategory.ORDER, AuditSeverity.INFO, "CUSTOMER_ORDER");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse confirm(String code, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        for (OrderItem item : order.getItems()) {
            productService.reserveStock(item.getProductCode(), item.getQuantity());
        }
        order.confirm(timeProvider.localDateTime());
        auditService.record(actor.username(), actor.roleLabel(), "CONFIRM_ORDER", order.getCode(), "Ordine confermato per " + order.getCustomer() + " - stock riservato", AuditCategory.ORDER, AuditSeverity.WARNING, "CUSTOMER_ORDER");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse fulfill(String code, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        order.fulfill(timeProvider.localDateTime());
        for (OrderItem item : order.getItems()) {
            inventoryService.fulfillReserved(item.getProductCode(), item.getQuantity(), "Evasione ordine " + order.getCode(), actor.username(), actor.roleLabel());
        }
        auditService.record(actor.username(), actor.roleLabel(), "FULFILL_ORDER", order.getCode(), "Ordine evaso per " + order.getCustomer(), AuditCategory.ORDER, AuditSeverity.INFO, "CUSTOMER_ORDER");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(String code, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(code, actor);
        if (actor.role() == UserRole.CUSTOMER && order.getStatus() != OrderStatus.DRAFT) {
            throw new ForbiddenException("Puoi annullare solo ordini in bozza.");
        }
        OrderStatus previousStatus = order.cancel(timeProvider.localDateTime());
        if (previousStatus == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                productService.releaseReservedStock(item.getProductCode(), item.getQuantity());
            }
        }
        auditService.record(actor.username(), actor.roleLabel(), "CANCEL_ORDER", order.getCode(), "Ordine annullato per " + order.getCustomer() + " - stato precedente " + previousStatus.getLabel(), AuditCategory.ORDER, AuditSeverity.WARNING, "CUSTOMER_ORDER");
        return OrderResponse.from(order);
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
        return OrderResponse.from(order);
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
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse approveReturn(String orderCode, String returnCode, OrderOperationRequests.ReturnReviewRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(orderCode, actor);
        OrderReturn orderReturn = requireReturn(order, returnCode);
        orderReturn.approve(actor.username(), request.note(), timeProvider.localDateTime());
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "APPROVE_RETURN", returnCode, "Reso approvato per ordine " + order.getCode(), AuditCategory.ORDER, AuditSeverity.WARNING, "ORDER_RETURN");
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse rejectReturn(String orderCode, String returnCode, OrderOperationRequests.ReturnRejectionRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(orderCode, actor);
        OrderReturn orderReturn = requireReturn(order, returnCode);
        orderReturn.reject(actor.username(), request.note(), timeProvider.localDateTime());
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "REJECT_RETURN", returnCode, "Reso rifiutato per ordine " + order.getCode() + " - " + request.note(), AuditCategory.ORDER, AuditSeverity.WARNING, "ORDER_RETURN");
        return OrderResponse.from(order);
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
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse refundReturn(String orderCode, String returnCode, OrderOperationRequests.ReturnRefundRequest request, AuthenticatedUser actor) {
        CustomerOrder order = requireOrderForTransition(orderCode, actor);
        OrderReturn orderReturn = requireReturn(order, returnCode);
        LocalDateTime changedAt = timeProvider.localDateTime();
        PaymentTransaction transaction = order.getPayment().recordRefund(
                codeGenerator.nextPaymentTransactionCode(),
                returnCode,
                request.amount(),
                request.reference(),
                request.reason(),
                changedAt,
                actor.username(),
                actor.roleLabel()
        );
        orderReturn.registerRefund(request.amount(), changedAt);
        repository.saveAndFlush(order);
        auditService.record(actor.username(), actor.roleLabel(), "REFUND_RETURN", returnCode, "Rimborso " + transaction.getCode() + " di " + transaction.getAmount() + " EUR per ordine " + order.getCode(), AuditCategory.ORDER, AuditSeverity.CRITICAL, "PAYMENT_TRANSACTION");
        return OrderResponse.from(order);
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
        return new OrderItem(product.getCode(), product.getName(), quantity, product.getDiscountedPrice());
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
        if (actor.role() != UserRole.CUSTOMER && hasText(request.customerCode())) {
            BusinessPartnerResponse partner = partnerService.requireActiveCustomer(request.customerCode());
            return new CustomerData(partner.displayName(), partner.code());
        }
        return new CustomerData(fallbackCustomer, "");
    }

    private CustomerOrder requireOrderForTransition(String code, AuthenticatedUser actor) {
        CustomerOrder order = repository.findByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("Ordine non trovato."));
        if (actor.role() == UserRole.CUSTOMER && !order.getCustomer().equalsIgnoreCase(actor.username())) {
            throw new ForbiddenException("Puoi operare solo sui tuoi ordini.");
        }
        return order;
    }

    private Specification<CustomerOrder> specification(String q, String customer) {
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
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private record CustomerData(String name, String code) {
    }
}
