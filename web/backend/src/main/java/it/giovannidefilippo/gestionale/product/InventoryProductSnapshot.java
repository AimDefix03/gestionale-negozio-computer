package it.giovannidefilippo.gestionale.product;

public record InventoryProductSnapshot(
        Long productId,
        String productCode,
        String productName,
        int quantity,
        int reservedQuantity,
        long version
) {
}
