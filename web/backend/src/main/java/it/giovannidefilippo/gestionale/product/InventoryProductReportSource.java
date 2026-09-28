package it.giovannidefilippo.gestionale.product;

import java.math.BigDecimal;

public record InventoryProductReportSource(
        String code,
        String name,
        ProductCategory category,
        String brand,
        String productType,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        BigDecimal price,
        BigDecimal discount,
        BigDecimal discountedPrice,
        BigDecimal lastPurchaseCost,
        BigDecimal averagePurchaseCost,
        int costedQuantity,
        int uncostedQuantity,
        BigDecimal costCoveragePercentage,
        BigDecimal knownInventoryCost,
        BigDecimal potentialGrossMarginOnCostedStock,
        boolean discontinued
) {
}
