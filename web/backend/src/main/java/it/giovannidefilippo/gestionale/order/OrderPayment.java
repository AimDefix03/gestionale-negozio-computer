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

    private LocalDateTime reconciledAt;

    @Column(length = 120)
    private String reconciledBy;

    @Column(length = 80)
    private String reconciledByRole;

    @Column(length = 120)
    private String reconciliationReference;

    @Column(length = 500)
    private String reconciliationReason;

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

    PaymentTransaction recordReceipt(String transactionCode, BigDecimal amount, String reference, String reason, LocalDateTime changedAt, String actor, String actorRole) {
        if (status == PaymentStatus.UNRECONCILED) {
            throw new IllegalStateException("Riconcilia il pagamento storico prima di registrare nuovi incassi.");
        }
        if (status == PaymentStatus.CANCELED || status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Il pagamento non accetta nuovi incassi.");
        }
        BigDecimal value = positiveMoney(amount);
        if (value.compareTo(getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("L'incasso supera il saldo residuo dell'ordine.");
        }
        PaymentTransaction transaction = PaymentTransaction.receipt(transactionCode, value, reference, reason, changedAt, actor, actorRole);
        addTransaction(transaction);
        synchronizeBalancesFromLedger();
        updateStatusAfterReceipt();
        updatedAt = changedAt;
        return transaction;
    }

    void reconcile(String transactionCode, BigDecimal verifiedPaidAmount, String reference, String reason, LocalDateTime changedAt, String actor, String actorRole) {
        if (status != PaymentStatus.UNRECONCILED) {
            throw new IllegalStateException("Il pagamento non richiede una riconciliazione.");
        }
        BigDecimal verifiedAmount = money(verifiedPaidAmount);
        if (verifiedAmount.compareTo(requestedAmount) > 0) {
            throw new IllegalArgumentException("L'importo verificato supera il totale dell'ordine.");
        }
        if (verifiedAmount.signum() > 0) {
            addTransaction(PaymentTransaction.reconciliation(
                    transactionCode,
                    verifiedAmount,
                    reference,
                    reason,
                    changedAt,
                    actor,
                    actorRole
            ));
        }
        synchronizeBalancesFromLedger();
        status = verifiedAmount.signum() == 0
                ? PaymentStatus.PENDING
                : verifiedAmount.compareTo(requestedAmount) == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID;
        reconciledAt = Objects.requireNonNull(changedAt);
        reconciledBy = required(actor, "L'operatore di riconciliazione e obbligatorio.");
        reconciledByRole = required(actorRole, "Il ruolo dell'operatore di riconciliazione e obbligatorio.");
        reconciliationReference = optional(reference);
        reconciliationReason = required(reason, "La motivazione della riconciliazione e obbligatoria.");
        updatedAt = changedAt;
    }

    PaymentTransaction recordRefund(String transactionCode, OrderReturn orderReturn, BigDecimal amount, String reference, String reason, LocalDateTime changedAt, String actor, String actorRole) {
        BigDecimal value = positiveMoney(amount);
        if (value.compareTo(getRefundableAmount()) > 0) {
            throw new IllegalArgumentException("Il rimborso supera l'importo netto incassato.");
        }
        PaymentTransaction transaction = PaymentTransaction.refund(transactionCode, orderReturn, value, reference, reason, changedAt, actor, actorRole);
        addTransaction(transaction);
        synchronizeBalancesFromLedger();
        status = refundedAmount.compareTo(paidAmount) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = changedAt;
        return transaction;
    }

    boolean requiresCancellationReversal() {
        requireCancellationState();
        return getNetPaidAmount().signum() > 0;
    }

    PaymentTransaction cancelForOrder(Long orderId, String transactionCode, String reference, String reason, LocalDateTime changedAt, String actor, String actorRole) {
        requireCancellationState();
        LocalDateTime operationTime = Objects.requireNonNull(changedAt);
        BigDecimal netPaidAmount = getNetPaidAmount();
        if (netPaidAmount.signum() == 0) {
            status = PaymentStatus.CANCELED;
            updatedAt = operationTime;
            return null;
        }
        PaymentTransaction transaction = PaymentTransaction.reversal(
                transactionCode,
                Objects.requireNonNull(orderId),
                netPaidAmount,
                required(reference, "Il riferimento dello storno e obbligatorio per annullare un ordine incassato."),
                reason,
                operationTime,
                actor,
                actorRole
        );
        addTransaction(transaction);
        synchronizeBalancesFromLedger();
        status = PaymentStatus.REFUNDED;
        updatedAt = operationTime;
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
    public BigDecimal getOutstandingAmount() {
        if (status == PaymentStatus.CANCELED || status == PaymentStatus.REFUNDED) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return requestedAmount.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);
    }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isReconciliationRequired() { return status == PaymentStatus.UNRECONCILED; }
    public LocalDateTime getReconciledAt() { return reconciledAt; }
    public String getReconciledBy() { return reconciledBy; }
    public String getReconciledByRole() { return reconciledByRole; }
    public String getReconciliationReference() { return reconciliationReference; }
    public String getReconciliationReason() { return reconciliationReason; }
    public List<PaymentTransaction> getTransactions() { return List.copyOf(transactions); }

    BigDecimal refundedForReturn(OrderReturn orderReturn) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == PaymentTransactionType.REFUND)
                .filter(transaction -> Objects.equals(transaction.getReturnId(), orderReturn.getId())
                        || transaction.getReturnCode() != null && transaction.getReturnCode().equalsIgnoreCase(orderReturn.getCode()))
                .map(PaymentTransaction::getAmount)
                .reduce(zero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void addTransaction(PaymentTransaction transaction) {
        transaction.assignPayment(this);
        transactions.add(transaction);
    }

    private void updateStatusAfterReceipt() {
        status = paidAmount.compareTo(requestedAmount) == 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID;
    }

    private void synchronizeBalancesFromLedger() {
        paidAmount = transactions.stream()
                .filter(transaction -> transaction.getType() == PaymentTransactionType.RECEIPT || transaction.getType() == PaymentTransactionType.RECONCILIATION)
                .map(PaymentTransaction::getAmount)
                .reduce(zero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        refundedAmount = transactions.stream()
                .filter(transaction -> transaction.getType() == PaymentTransactionType.REFUND || transaction.getType() == PaymentTransactionType.REVERSAL)
                .map(PaymentTransaction::getAmount)
                .reduce(zero(), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private void requireCancellationState() {
        if (status == PaymentStatus.UNRECONCILED) {
            throw new IllegalStateException("Riconcilia il pagamento storico prima di annullare l'ordine.");
        }
        if (status != PaymentStatus.PENDING
                && status != PaymentStatus.FAILED
                && status != PaymentStatus.PARTIALLY_PAID
                && status != PaymentStatus.PAID) {
            throw new IllegalStateException("Lo stato del pagamento non consente l'annullamento dell'ordine.");
        }
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
