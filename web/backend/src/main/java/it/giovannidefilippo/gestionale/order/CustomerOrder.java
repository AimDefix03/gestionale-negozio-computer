package it.giovannidefilippo.gestionale.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String customer;

    private String customerCode;

    private Long customerAccountId;

    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderCustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderOwnershipStatus ownershipStatus;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String paymentMethod;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, optional = false)
    private OrderPayment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<OrderReturn> returns = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime statusChangedAt;

    @Column(length = 120)
    private String cancellationReference;

    @Column(length = 500)
    private String cancellationReason;

    private LocalDateTime canceledAt;

    @Column(length = 120)
    private String canceledBy;

    @Column(length = 80)
    private String canceledByRole;

    protected CustomerOrder() {
    }

    CustomerOrder(
            String code,
            String customer,
            String customerCode,
            Long customerAccountId,
            Long partnerId,
            OrderCustomerType customerType,
            PaymentMethod paymentMethod,
            List<OrderItem> items,
            LocalDateTime timestamp
    ) {
        this.code = code;
        this.customer = customer.trim();
        this.customerCode = optional(customerCode);
        this.customerAccountId = customerAccountId;
        this.partnerId = partnerId;
        this.customerType = Objects.requireNonNull(customerType);
        this.ownershipStatus = ownershipStatus(customerAccountId, partnerId);
        this.timestamp = Objects.requireNonNull(timestamp);
        PaymentMethod selectedPaymentMethod = Objects.requireNonNull(paymentMethod);
        if (!selectedPaymentMethod.isSelectable()) {
            throw new IllegalArgumentException("Metodo di pagamento non selezionabile per un nuovo ordine.");
        }
        this.paymentMethod = selectedPaymentMethod.getLabel();
        this.total = items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.status = OrderStatus.DRAFT;
        this.statusChangedAt = this.timestamp;
        items.forEach(this::addItem);
        this.payment = new OrderPayment(selectedPaymentMethod, this.total, this.timestamp);
        this.payment.assignOrder(this);
    }

    CustomerOrder(String code, String customer, String customerCode, PaymentMethod paymentMethod, List<OrderItem> items, LocalDateTime timestamp) {
        this(code, customer, customerCode, null, null, OrderCustomerType.LEGACY_UNRESOLVED, paymentMethod, items, timestamp);
    }

    private void addItem(OrderItem item) {
        item.assignOrder(this);
        items.add(item);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getCustomer() { return customer; }
    public String getCustomerCode() { return customerCode; }
    public Long getCustomerAccountId() { return customerAccountId; }
    public Long getPartnerId() { return partnerId; }
    public OrderCustomerType getCustomerType() { return customerType; }
    public OrderOwnershipStatus getOwnershipStatus() { return ownershipStatus; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPaymentMethod() { return paymentMethod; }
    public OrderPayment getPayment() { return payment; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public List<OrderReturn> getReturns() { return List.copyOf(returns); }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getStatusChangedAt() { return statusChangedAt; }
    public String getCancellationReference() { return cancellationReference; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public String getCanceledBy() { return canceledBy; }
    public String getCanceledByRole() { return canceledByRole; }

    void confirm(LocalDateTime changedAt) {
        requireStatus(OrderStatus.DRAFT, "Puoi confermare solo un ordine in bozza.");
        changeStatus(OrderStatus.CONFIRMED, changedAt);
    }

    void fulfill(LocalDateTime changedAt) {
        requireStatus(OrderStatus.CONFIRMED, "Puoi evadere solo un ordine confermato.");
        changeStatus(OrderStatus.FULFILLED, changedAt);
    }

    boolean requiresCancellationReversal() {
        requireCancellationStatus();
        return payment.requiresCancellationReversal();
    }

    OrderCancellationResult cancel(String transactionCode, String reference, String reason, LocalDateTime changedAt, String actor, String actorRole) {
        requireCancellationStatus();
        OrderStatus previousStatus = status;
        String cancellationReason = required(reason, "La motivazione dell'annullamento e obbligatoria.");
        String cancellationActor = required(actor, "L'operatore dell'annullamento e obbligatorio.");
        String cancellationActorRole = required(actorRole, "Il ruolo dell'operatore dell'annullamento e obbligatorio.");
        PaymentTransaction reversal = payment.cancelForOrder(id, transactionCode, reference, cancellationReason, changedAt, cancellationActor, cancellationActorRole);
        this.cancellationReference = optional(reference);
        this.cancellationReason = cancellationReason;
        this.canceledAt = Objects.requireNonNull(changedAt);
        this.canceledBy = cancellationActor;
        this.canceledByRole = cancellationActorRole;
        changeStatus(OrderStatus.CANCELED, changedAt);
        return new OrderCancellationResult(previousStatus, reversal);
    }

    void addReturn(OrderReturn orderReturn) {
        if (status != OrderStatus.FULFILLED) {
            throw new IllegalStateException("Puoi richiedere un reso solo per un ordine evaso.");
        }
        orderReturn.assignOrder(this);
        returns.add(orderReturn);
    }

    int returnedOrReservedQuantity(String productCode) {
        return returns.stream().mapToInt(orderReturn -> orderReturn.reservedQuantityFor(productCode)).sum();
    }

    public boolean isFulfilled() {
        return status == OrderStatus.FULFILLED;
    }

    boolean isOwnedBy(long accountId) {
        return customerAccountId != null && customerAccountId == accountId;
    }

    private void requireStatus(OrderStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void requireCancellationStatus() {
        if (status != OrderStatus.DRAFT && status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Puoi annullare solo ordini in bozza o confermati.");
        }
    }

    private void changeStatus(OrderStatus newStatus, LocalDateTime changedAt) {
        this.status = newStatus;
        this.statusChangedAt = Objects.requireNonNull(changedAt);
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static OrderOwnershipStatus ownershipStatus(Long accountId, Long partnerId) {
        if (accountId != null) {
            return OrderOwnershipStatus.ACCOUNT;
        }
        if (partnerId != null) {
            return OrderOwnershipStatus.PARTNER;
        }
        return OrderOwnershipStatus.UNRESOLVED;
    }
}
