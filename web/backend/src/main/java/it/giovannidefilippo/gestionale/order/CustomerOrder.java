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

    protected CustomerOrder() {
    }

    CustomerOrder(String code, String customer, String customerCode, PaymentMethod paymentMethod, List<OrderItem> items, LocalDateTime timestamp) {
        this.code = code;
        this.customer = customer.trim();
        this.customerCode = optional(customerCode);
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

    private void addItem(OrderItem item) {
        item.assignOrder(this);
        items.add(item);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getCustomer() { return customer; }
    public String getCustomerCode() { return customerCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPaymentMethod() { return paymentMethod; }
    public OrderPayment getPayment() { return payment; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public List<OrderReturn> getReturns() { return List.copyOf(returns); }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getStatusChangedAt() { return statusChangedAt; }

    void confirm(LocalDateTime changedAt) {
        requireStatus(OrderStatus.DRAFT, "Puoi confermare solo un ordine in bozza.");
        changeStatus(OrderStatus.CONFIRMED, changedAt);
    }

    void fulfill(LocalDateTime changedAt) {
        requireStatus(OrderStatus.CONFIRMED, "Puoi evadere solo un ordine confermato.");
        changeStatus(OrderStatus.FULFILLED, changedAt);
    }

    OrderStatus cancel(LocalDateTime changedAt) {
        if (status != OrderStatus.DRAFT && status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Puoi annullare solo ordini in bozza o confermati.");
        }
        OrderStatus previousStatus = status;
        changeStatus(OrderStatus.CANCELED, changedAt);
        payment.cancel(changedAt);
        return previousStatus;
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

    private void requireStatus(OrderStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void changeStatus(OrderStatus newStatus, LocalDateTime changedAt) {
        this.status = newStatus;
        this.statusChangedAt = Objects.requireNonNull(changedAt);
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
