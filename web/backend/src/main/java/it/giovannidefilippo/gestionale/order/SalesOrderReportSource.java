package it.giovannidefilippo.gestionale.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SalesOrderReportSource(
        String code,
        String customer,
        LocalDateTime timestamp,
        OrderStatus status,
        BigDecimal total,
        BigDecimal paidAmount,
        BigDecimal refundedAmount,
        BigDecimal netPaidAmount,
        BigDecimal outstandingAmount,
        List<SalesOrderItemReportSource> items
) {
}
