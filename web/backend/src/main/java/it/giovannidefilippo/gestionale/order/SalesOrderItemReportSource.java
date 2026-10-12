package it.giovannidefilippo.gestionale.order;

import java.math.BigDecimal;

public record SalesOrderItemReportSource(
        String productCode,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
