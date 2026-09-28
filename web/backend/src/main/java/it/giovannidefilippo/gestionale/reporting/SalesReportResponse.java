package it.giovannidefilippo.gestionale.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record SalesReportResponse(
        OffsetDateTime generatedAt,
        LocalDate from,
        LocalDate to,
        String status,
        String statusLabel,
        int orderCount,
        BigDecimal orderValue,
        BigDecimal paidAmount,
        BigDecimal refundedAmount,
        BigDecimal netCollectedAmount,
        BigDecimal outstandingAmount,
        BigDecimal averageOrderValue,
        List<SalesOrderRow> orders,
        List<TopProductRow> topProducts
) {
    public record SalesOrderRow(
            String code,
            OffsetDateTime timestamp,
            String customer,
            String status,
            String statusLabel,
            BigDecimal total,
            BigDecimal paidAmount,
            BigDecimal refundedAmount,
            BigDecimal netCollectedAmount,
            BigDecimal outstandingAmount
    ) {
    }

    public record TopProductRow(
            String productCode,
            String productName,
            int quantity,
            BigDecimal orderValue
    ) {
    }
}
