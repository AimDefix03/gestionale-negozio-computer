package it.giovannidefilippo.gestionale.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryReportResponse(
        LocalDateTime generatedAt,
        int productCount,
        long physicalUnits,
        long reservedUnits,
        long availableUnits,
        BigDecimal inventoryValue,
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
            BigDecimal stockValue,
            boolean discontinued,
            String stockStatus,
            String stockStatusLabel
    ) {
    }
}
