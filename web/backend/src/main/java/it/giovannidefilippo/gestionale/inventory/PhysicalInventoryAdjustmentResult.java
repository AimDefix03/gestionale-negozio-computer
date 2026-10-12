package it.giovannidefilippo.gestionale.inventory;

record PhysicalInventoryAdjustmentResult(
        int previousQuantity,
        int newQuantity,
        int reservedQuantity,
        Long stockMovementId
) {
}
