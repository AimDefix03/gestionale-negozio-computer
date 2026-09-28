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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private OrderPayment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 120)
    private String reference;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(length = 40)
    private String returnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id")
    private OrderReturn orderReturn;

    @Column(name = "cancellation_order_id")
    private Long cancellationOrderId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_payment_id", unique = true)
    private OrderPayment reconciliationPayment;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false, length = 120)
    private String recordedBy;

    @Column(nullable = false, length = 80)
    private String recordedByRole;

    protected PaymentTransaction() {
    }

    private PaymentTransaction(String code, PaymentTransactionType type, BigDecimal amount, String reference, String reason, OrderReturn orderReturn, Long cancellationOrderId, LocalDateTime recordedAt, String recordedBy, String recordedByRole) {
        this.code = required(code, "Il codice movimento e obbligatorio.");
        this.type = Objects.requireNonNull(type);
        this.amount = positiveMoney(amount);
        this.reference = optional(reference);
        this.reason = required(reason, "La causale del movimento e obbligatoria.");
        this.orderReturn = orderReturn;
        this.returnCode = orderReturn == null ? null : orderReturn.getCode();
        this.cancellationOrderId = cancellationOrderId;
        this.recordedAt = Objects.requireNonNull(recordedAt);
        this.recordedBy = required(recordedBy, "L'operatore e obbligatorio.");
        this.recordedByRole = required(recordedByRole, "Il ruolo operatore e obbligatorio.");
    }

    static PaymentTransaction receipt(String code, BigDecimal amount, String reference, String reason, LocalDateTime recordedAt, String recordedBy, String recordedByRole) {
        return new PaymentTransaction(code, PaymentTransactionType.RECEIPT, amount, reference, reason, null, null, recordedAt, recordedBy, recordedByRole);
    }

    static PaymentTransaction refund(String code, OrderReturn orderReturn, BigDecimal amount, String reference, String reason, LocalDateTime recordedAt, String recordedBy, String recordedByRole) {
        return new PaymentTransaction(code, PaymentTransactionType.REFUND, amount, reference, reason, Objects.requireNonNull(orderReturn), null, recordedAt, recordedBy, recordedByRole);
    }

    static PaymentTransaction reversal(String code, Long cancellationOrderId, BigDecimal amount, String reference, String reason, LocalDateTime recordedAt, String recordedBy, String recordedByRole) {
        return new PaymentTransaction(code, PaymentTransactionType.REVERSAL, amount, reference, reason, null, Objects.requireNonNull(cancellationOrderId), recordedAt, recordedBy, recordedByRole);
    }

    static PaymentTransaction reconciliation(String code, BigDecimal amount, String reference, String reason, LocalDateTime recordedAt, String recordedBy, String recordedByRole) {
        return new PaymentTransaction(code, PaymentTransactionType.RECONCILIATION, amount, reference, reason, null, null, recordedAt, recordedBy, recordedByRole);
    }

    void assignPayment(OrderPayment payment) {
        if (this.payment != null && this.payment != payment) {
            throw new IllegalStateException("Il movimento e gia associato a un pagamento.");
        }
        this.payment = Objects.requireNonNull(payment);
        if (type == PaymentTransactionType.RECONCILIATION) {
            reconciliationPayment = payment;
        }
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public PaymentTransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
    public String getReason() { return reason; }
    public String getReturnCode() { return returnCode; }
    public Long getReturnId() { return orderReturn == null ? null : orderReturn.getId(); }
    public Long getCancellationOrderId() { return cancellationOrderId; }
    public Long getReconciliationPaymentId() { return reconciliationPayment == null ? null : reconciliationPayment.getId(); }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public String getRecordedByRole() { return recordedByRole; }

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
        return value == null || value.isBlank() ? null : value.trim();
    }
}
