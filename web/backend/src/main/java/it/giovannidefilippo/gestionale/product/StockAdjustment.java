package it.giovannidefilippo.gestionale.product;

public record StockAdjustment(
        Long productId,
        String productCode,
        String productName,
        int previousQuantity,
        int newQuantity
) {
}
