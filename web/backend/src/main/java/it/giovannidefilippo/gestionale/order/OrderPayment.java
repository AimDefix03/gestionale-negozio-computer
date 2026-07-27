package it.giovannidefilippo.gestionale.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "order_payments")
public class OrderPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentMethod method;

    private String methodDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    protected OrderPayment() {
    }

    OrderPayment(PaymentMethod method, BigDecimal requestedAmount, LocalDateTime createdAt) {
        this.method = Objects.requireNonNull(method);
        this.methodDetails = "";
        this.status = PaymentStatus.PENDING;
        this.requestedAmount = money(requestedAmount);
        this.paidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.refundedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.currency = "EUR";
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = this.createdAt;
    }

    void assignOrder(CustomerOrder order) {
        if (this.order != null && this.order != order) {
            throw new IllegalStateException("Il pagamento e gia associato a un ordine.");
        }
        this.order = Objects.requireNonNull(order);
    }

    void cancel(LocalDateTime changedAt) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Puoi annullare direttamente solo un pagamento in attesa.");
        }
        status = PaymentStatus.CANCELED;
        updatedAt = Objects.requireNonNull(changedAt);
    }

    PaymentTransaction recordReceipt(String transactionCode, BigDecimal amount, String reference, String reason, LocalDateTime changedAt, String actor, String actorRole) {
        if (status == PaymentStatus.CANCELED || status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Il pagamento non accetta nuovi incassi.");
        }
        BigDecimal value = positiveMoney(amount);
        if (value.compareTo(getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("L'incasso supera il saldo residuo dell'ordine.");
        }
        PaymentTransaction transaction = new PaymentTransaction(transactionCode, PaymentTransactionType.RECEIPT, value, reference, reason, "", changedAt, actor, actorRole);
        addTransaction(transaction);
        paidAmount = paidAmount.add(value).setScale(2, RoundingMode.HALF_UP);
        updateStatusAfterReceipt();
        updatedAt = changedAt;
        return transaction;
    }

    PaymentTransaction recordRefund(String transactionCode, String returnCode, BigDecimal amount, String reference, String reason, LocalDateTime changedAt, String actor, String actorRole) {
        BigDecimal value = positiveMoney(amount);
        if (value.compareTo(getRefundableAmount()) > 0) {
            throw new IllegalArgumentException("Il rimborso supera l'importo netto incassato.");
        }
        PaymentTransaction transaction = new PaymentTransaction(transactionCode, PaymentTransactionType.REFUND, value, reference, reason, returnCode, changedAt, actor, actorRole);
        addTransaction(transaction);
        refundedAmount = refundedAmount.add(value).setScale(2, RoundingMode.HALF_UP);
        status = refundedAmount.compareTo(paidAmount) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = changedAt;
        return transaction;
    }

    public Long getId() { return id; }
    public PaymentMethod getMethod() { return method; }
    public String getMethodDetails() { return methodDetails; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public BigDecimal getNetPaidAmount() { return paidAmount.subtract(refundedAmount).setScale(2, RoundingMode.HALF_UP); }
    public BigDecimal getRefundableAmount() { return getNetPaidAmount(); }
    public BigDecimal getOutstandingAmount() { return requestedAmount.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP); }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<PaymentTransaction> getTransactions() { return List.copyOf(transactions); }

    private void addTransaction(PaymentTransaction transaction) {
        transaction.assignPayment(this);
        transactions.add(transaction);
    }

    private void updateStatusAfterReceipt() {
        status = paidAmount.compareTo(requestedAmount) == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID;
    }

    private static BigDecimal positiveMoney(BigDecimal value) {
        BigDecimal amount = money(value);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("L'importo deve essere maggiore di zero.");
        }
        return amount;
    }

    private static BigDecimal money(BigDecimal value) {
        BigDecimal amount = Objects.requireNonNull(value).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("L'importo richiesto non puo essere negativo.");
        }
        return amount;
    }
}
