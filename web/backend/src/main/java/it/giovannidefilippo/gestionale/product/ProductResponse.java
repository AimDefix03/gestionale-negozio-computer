package it.giovannidefilippo.gestionale.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String code,
        String name,
        String description,
        ProductCategory category,
        String brand,
        String productType,
        String usageContext,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        BigDecimal price,
        BigDecimal discount,
        BigDecimal discountedPrice,
        boolean discontinued
) {
}
