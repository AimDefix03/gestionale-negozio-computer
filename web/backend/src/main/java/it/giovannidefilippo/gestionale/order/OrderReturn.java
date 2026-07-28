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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "order_returns")
public class OrderReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderReturnStatus status;

    @Column(nullable = false, length = 500)
    private String reason;

    @OneToMany(mappedBy = "orderReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<OrderReturnItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column(nullable = false, length = 120)
    private String requestedBy;

    @Column(nullable = false, length = 80)
    private String requestedByRole;

    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String reviewNote;
    private LocalDateTime receivedAt;
    private String receivedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    protected OrderReturn() {
    }

    OrderReturn(String code, String reason, List<OrderReturnItem> items, LocalDateTime requestedAt, String requestedBy, String requestedByRole) {
        this.code = required(code, "Il codice reso e obbligatorio.");
        this.status = OrderReturnStatus.REQUESTED;
        this.reason = required(reason, "La motivazione del reso e obbligatoria.");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Il reso deve contenere almeno un prodotto.");
        }
        items.forEach(this::addItem);
        this.totalAmount = this.items.stream().map(OrderReturnItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        this.refundedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.requestedAt = Objects.requireNonNull(requestedAt);
        this.requestedBy = required(requestedBy, "Il richiedente e obbligatorio.");
        this.requestedByRole = required(requestedByRole, "Il ruolo richiedente e obbligatorio.");
        this.updatedAt = this.requestedAt;
    }

    void assignOrder(CustomerOrder order) {
        if (this.order != null && this.order != order) {
            throw new IllegalStateException("Il reso e gia associato a un ordine.");
        }
        this.order = Objects.requireNonNull(order);
    }

    void approve(String actor, String note, LocalDateTime changedAt) {
        requireStatus(OrderReturnStatus.REQUESTED, "Puoi approvare solo un reso richiesto.");
        status = OrderReturnStatus.APPROVED;
        review(actor, note, changedAt);
    }

    void reject(String actor, String note, LocalDateTime changedAt) {
        requireStatus(OrderReturnStatus.REQUESTED, "Puoi rifiutare solo un reso richiesto.");
        status = OrderReturnStatus.REJECTED;
        review(actor, required(note, "La motivazione del rifiuto e obbligatoria."), changedAt);
    }

    void receive(String actor, LocalDateTime changedAt) {
        requireStatus(OrderReturnStatus.APPROVED, "Puoi ricevere solo un reso approvato.");
        status = OrderReturnStatus.RECEIVED;
        receivedAt = Objects.requireNonNull(changedAt);
        receivedBy = required(actor, "L'operatore e obbligatorio.");
        updatedAt = changedAt;
    }

    void registerRefund(BigDecimal amount, LocalDateTime changedAt) {
        if (status != OrderReturnStatus.RECEIVED && status != OrderReturnStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Puoi rimborsare solo un reso ricevuto.");
        }
        BigDecimal value = positiveMoney(amount);
        if (value.compareTo(getRefundableAmount()) > 0) {
            throw new IllegalArgumentException("Il rimborso supera il valore residuo del reso.");
        }
        refundedAmount = refundedAmount.add(value).setScale(2, RoundingMode.HALF_UP);
        status = refundedAmount.compareTo(totalAmount) == 0 ? OrderReturnStatus.REFUNDED : OrderReturnStatus.PARTIALLY_REFUNDED;
        updatedAt = Objects.requireNonNull(changedAt);
    }

    int reservedQuantityFor(String productCode) {
        if (!status.reservesQuantity()) {
            return 0;
        }
        return items.stream().filter(item -> item.getProductCode().equalsIgnoreCase(productCode)).mapToInt(OrderReturnItem::getQuantity).sum();
    }

    public String getCode() { return code; }
    public OrderReturnStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public List<OrderReturnItem> getItems() { return List.copyOf(items); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public BigDecimal getRefundableAmount() { return totalAmount.subtract(refundedAmount).setScale(2, RoundingMode.HALF_UP); }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public String getRequestedBy() { return requestedBy; }
    public String getRequestedByRole() { return requestedByRole; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public String getReceivedBy() { return receivedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    private void addItem(OrderReturnItem item) {
        item.assignReturn(this);
        items.add(item);
    }

    private void review(String actor, String note, LocalDateTime changedAt) {
        reviewedBy = required(actor, "L'operatore e obbligatorio.");
        reviewNote = optional(note);
        reviewedAt = Objects.requireNonNull(changedAt);
        updatedAt = changedAt;
    }

    private void requireStatus(OrderReturnStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static BigDecimal positiveMoney(BigDecimal value) {
        BigDecimal amount = Objects.requireNonNull(value).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("L'importo deve essere maggiore di zero.");
        }
        return amount;
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
