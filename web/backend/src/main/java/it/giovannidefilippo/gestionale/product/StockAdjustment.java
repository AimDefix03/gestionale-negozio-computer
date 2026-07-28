package it.giovannidefilippo.gestionale.product;

public record StockAdjustment(
        String productCode,
        String productName,
        int previousQuantity,
        int newQuantity
) {
}
