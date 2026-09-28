package it.giovannidefilippo.gestionale.inventory;

record PhysicalInventoryAdjustmentCommand(
        Long sessionId,
        String sessionCode,
        Long itemId,
        Long productId,
        String productCode,
        String productName,
        int differenceQuantity,
        String approvalReason,
        String actor,
        String role
) {
}
