package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.common.BusinessTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class FinancialReconciliationService {
    private final FinancialReconciliationQueryRepository repository;
    private final FinancialReconciliationMetrics metrics;
    private final TimeProvider timeProvider;

    FinancialReconciliationService(FinancialReconciliationQueryRepository repository, FinancialReconciliationMetrics metrics, TimeProvider timeProvider) {
        this.repository = repository;
        this.metrics = metrics;
        this.timeProvider = timeProvider;
    }

    public FinancialReconciliationResponse reconcile() {
        List<FinancialReconciliationQueryRepository.PaymentSnapshot> payments = repository.payments();
        List<FinancialReconciliationQueryRepository.ReturnSnapshot> returns = repository.returns();
        List<FinancialReconciliationResponse.FinancialMismatch> mismatches = new ArrayList<>();
        payments.forEach(payment -> inspectPayment(payment, mismatches));
        returns.forEach(orderReturn -> inspectReturn(orderReturn, mismatches));

        Map<FinancialMismatchType, Long> counts = new EnumMap<>(FinancialMismatchType.class);
        for (FinancialMismatchType type : FinancialMismatchType.values()) {
            counts.put(type, mismatches.stream().filter(mismatch -> mismatch.type() == type).count());
        }
        FinancialReconciliationResponse report = new FinancialReconciliationResponse(
                BusinessTime.utcOffset(timeProvider.localDateTime()),
                mismatches.isEmpty(),
                payments.size(),
                returns.size(),
                mismatches.size(),
                Map.copyOf(counts),
                List.copyOf(mismatches)
        );
        metrics.recordSuccess(report);
        return report;
    }

    void recordFailure() {
        metrics.recordFailure();
    }

    private static void inspectPayment(
            FinancialReconciliationQueryRepository.PaymentSnapshot payment,
            List<FinancialReconciliationResponse.FinancialMismatch> mismatches
    ) {
        if (!same(payment.paidAmount(), payment.ledgerPaid())) {
            add(mismatches, FinancialMismatchType.PAYMENT_PAID_LEDGER_DRIFT, "PAYMENT", payment.id(), payment.orderCode(), payment.orderCode(), payment.paidAmount(), payment.ledgerPaid(), "Il totale incassato materializzato diverge dal ledger.");
        }
        if (!same(payment.refundedAmount(), payment.ledgerRefunded())) {
            add(mismatches, FinancialMismatchType.PAYMENT_REFUNDED_LEDGER_DRIFT, "PAYMENT", payment.id(), payment.orderCode(), payment.orderCode(), payment.refundedAmount(), payment.ledgerRefunded(), "Il totale rimborsato materializzato diverge dal ledger.");
        }
        if ("UNRECONCILED".equals(payment.status())) {
            add(mismatches, FinancialMismatchType.PAYMENT_UNRECONCILED, "PAYMENT", payment.id(), payment.orderCode(), payment.orderCode(), payment.paidAmount(), payment.ledgerPaid(), "Il pagamento storico richiede evidenze e riconciliazione manuale.");
        } else if (!validPaymentState(payment)) {
            add(mismatches, FinancialMismatchType.PAYMENT_INVALID_STATE, "PAYMENT", payment.id(), payment.orderCode(), payment.orderCode(), payment.paidAmount().subtract(payment.refundedAmount()), payment.ledgerPaid().subtract(payment.ledgerRefunded()), "Stato del pagamento incompatibile con importi e stato ordine.");
        }
        if (payment.updatedAt().isBefore(payment.createdAt())
                || payment.reconciledAt() != null && (payment.reconciledAt().isBefore(payment.createdAt()) || payment.reconciledAt().isAfter(payment.updatedAt()))
                || payment.invalidTransactionTimes() > 0) {
            add(mismatches, FinancialMismatchType.PAYMENT_INVALID_TIMELINE, "PAYMENT", payment.id(), payment.orderCode(), payment.orderCode(), null, null, "Sequenza temporale del pagamento o dei movimenti non valida.");
        }
        if (payment.invalidLinks() > 0) {
            add(mismatches, FinancialMismatchType.PAYMENT_INVALID_TRANSACTION_LINK, "PAYMENT", payment.id(), payment.orderCode(), payment.orderCode(), null, null, "Uno o piu movimenti hanno collegamenti finanziari incompatibili.");
        }
    }

    private static void inspectReturn(
            FinancialReconciliationQueryRepository.ReturnSnapshot orderReturn,
            List<FinancialReconciliationResponse.FinancialMismatch> mismatches
    ) {
        if (!same(orderReturn.refundedAmount(), orderReturn.ledgerRefunded())) {
            add(mismatches, FinancialMismatchType.RETURN_REFUNDED_LEDGER_DRIFT, "RETURN", orderReturn.id(), orderReturn.code(), orderReturn.orderCode(), orderReturn.refundedAmount(), orderReturn.ledgerRefunded(), "Il totale rimborsato del reso diverge dai movimenti collegati.");
        }
        if (!validReturnState(orderReturn)) {
            add(mismatches, FinancialMismatchType.RETURN_INVALID_STATE, "RETURN", orderReturn.id(), orderReturn.code(), orderReturn.orderCode(), orderReturn.refundedAmount(), orderReturn.ledgerRefunded(), "Stato del reso incompatibile con il totale rimborsato.");
        }
        if (!validReturnTimeline(orderReturn)) {
            add(mismatches, FinancialMismatchType.RETURN_INVALID_TIMELINE, "RETURN", orderReturn.id(), orderReturn.code(), orderReturn.orderCode(), null, null, "Sequenza temporale del reso non valida.");
        }
    }

    private static boolean validPaymentState(FinancialReconciliationQueryRepository.PaymentSnapshot payment) {
        BigDecimal paid = payment.paidAmount();
        BigDecimal refunded = payment.refundedAmount();
        if (paid.signum() < 0 || refunded.signum() < 0 || paid.compareTo(payment.requestedAmount()) > 0 || refunded.compareTo(paid) > 0) {
            return false;
        }
        if ("CANCELED".equals(payment.orderStatus())) {
            return paid.signum() == 0 && refunded.signum() == 0 && "CANCELED".equals(payment.status())
                    || paid.signum() > 0 && paid.compareTo(refunded) == 0 && "REFUNDED".equals(payment.status());
        }
        if (refunded.signum() > 0) {
            return refunded.compareTo(paid) == 0 ? "REFUNDED".equals(payment.status()) : "PARTIALLY_REFUNDED".equals(payment.status());
        }
        if (paid.signum() == 0) {
            return "PENDING".equals(payment.status()) || "FAILED".equals(payment.status());
        }
        return paid.compareTo(payment.requestedAmount()) == 0 ? "PAID".equals(payment.status()) : "PARTIALLY_PAID".equals(payment.status());
    }

    private static boolean validReturnState(FinancialReconciliationQueryRepository.ReturnSnapshot orderReturn) {
        BigDecimal refunded = orderReturn.refundedAmount();
        if (refunded.signum() < 0 || refunded.compareTo(orderReturn.totalAmount()) > 0) {
            return false;
        }
        if (refunded.signum() == 0) {
            return !"PARTIALLY_REFUNDED".equals(orderReturn.status()) && !"REFUNDED".equals(orderReturn.status());
        }
        return refunded.compareTo(orderReturn.totalAmount()) == 0
                ? "REFUNDED".equals(orderReturn.status())
                : "PARTIALLY_REFUNDED".equals(orderReturn.status());
    }

    private static boolean validReturnTimeline(FinancialReconciliationQueryRepository.ReturnSnapshot orderReturn) {
        if (orderReturn.updatedAt().isBefore(orderReturn.requestedAt())) {
            return false;
        }
        if (orderReturn.reviewedAt() != null && (orderReturn.reviewedAt().isBefore(orderReturn.requestedAt()) || orderReturn.reviewedAt().isAfter(orderReturn.updatedAt()))) {
            return false;
        }
        if (orderReturn.receivedAt() != null && (orderReturn.receivedAt().isBefore(orderReturn.requestedAt()) || orderReturn.receivedAt().isAfter(orderReturn.updatedAt()))) {
            return false;
        }
        return orderReturn.receivedAt() == null || orderReturn.reviewedAt() != null && !orderReturn.receivedAt().isBefore(orderReturn.reviewedAt());
    }

    private static boolean same(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) == 0;
    }

    private static void add(
            List<FinancialReconciliationResponse.FinancialMismatch> mismatches,
            FinancialMismatchType type,
            String aggregateType,
            Long aggregateId,
            String aggregateCode,
            String orderCode,
            BigDecimal materializedAmount,
            BigDecimal ledgerAmount,
            String detail
    ) {
        mismatches.add(new FinancialReconciliationResponse.FinancialMismatch(type, aggregateType, aggregateId, aggregateCode, orderCode, materializedAmount, ledgerAmount, detail));
    }
}
