package it.giovannidefilippo.gestionale.product;

import java.math.BigDecimal;

public record ProductDashboardSummary(
        long products,
        BigDecimal inventoryValue,
        long lowStock,
        long outOfStock
) {
}
