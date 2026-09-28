package it.giovannidefilippo.gestionale.reporting;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record InventoryReportResponse(
        OffsetDateTime generatedAt,
        int productCount,
        long physicalUnits,
        long reservedUnits,
        long availableUnits,
        BigDecimal potentialRetailStockValue,
        BigDecimal knownInventoryCostValue,
        BigDecimal potentialGrossMarginOnCostedStock,
        long costedUnits,
        long uncostedUnits,
        BigDecimal costCoveragePercentage,
        long lowStockCount,
        long outOfStockCount,
        long discontinuedCount,
        List<InventoryProductRow> products
) {
    public record InventoryProductRow(
            String code,
            String name,
            String category,
            String categoryLabel,
            String brand,
            String productType,
            int quantity,
            int reservedQuantity,
            int availableQuantity,
            BigDecimal price,
            BigDecimal discount,
            BigDecimal discountedPrice,
            BigDecimal potentialRetailValue,
            BigDecimal lastPurchaseCost,
            BigDecimal averagePurchaseCost,
            int costedQuantity,
            int uncostedQuantity,
            BigDecimal costCoveragePercentage,
            BigDecimal knownInventoryCost,
            BigDecimal potentialGrossMarginOnCostedStock,
            boolean discontinued,
            String stockStatus,
            String stockStatusLabel
    ) {
    }
}
