package it.giovannidefilippo.gestionale.product;

import java.math.BigDecimal;

public record CostedStockReceipt(
        Long productId,
        String productCode,
        String productName,
        int previousQuantity,
        int newQuantity,
        int previousCostedQuantity,
        int newCostedQuantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        BigDecimal previousAverageCost,
        BigDecimal newAverageCost
) {
}
