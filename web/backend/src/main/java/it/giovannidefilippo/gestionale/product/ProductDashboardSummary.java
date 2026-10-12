package it.giovannidefilippo.gestionale.product;

import java.math.BigDecimal;

public record ProductDashboardSummary(
        long products,
        BigDecimal potentialRetailStockValue,
        BigDecimal knownInventoryCostValue,
        BigDecimal potentialGrossMarginOnCostedStock,
        long costedUnits,
        long uncostedUnits,
        BigDecimal costCoveragePercentage,
        long lowStock,
        long outOfStock
) {
}
