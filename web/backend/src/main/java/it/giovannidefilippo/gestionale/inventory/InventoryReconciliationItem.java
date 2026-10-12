package it.giovannidefilippo.gestionale.inventory;

public record InventoryReconciliationItem(
        Long productId,
        String productCode,
        String productName,
        int physicalQuantity,
        int ledgerQuantity,
        int reservedQuantity,
        long authoritativeMovements,
        long legacyMovements,
        InventoryReconciliationStatus status,
        String statusLabel
) {
}
