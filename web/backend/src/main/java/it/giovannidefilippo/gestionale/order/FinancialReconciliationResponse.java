package it.giovannidefilippo.gestionale.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record FinancialReconciliationResponse(
        OffsetDateTime generatedAt,
        boolean balanced,
        long checkedPayments,
        long checkedReturns,
        long mismatchCount,
        Map<FinancialMismatchType, Long> counts,
        List<FinancialMismatch> mismatches
) {
    public record FinancialMismatch(
            FinancialMismatchType type,
            String aggregateType,
            Long aggregateId,
            String aggregateCode,
            String orderCode,
            BigDecimal materializedAmount,
            BigDecimal ledgerAmount,
            String detail
    ) {
    }
}
