package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.time.OffsetDateTime;

public record PhysicalInventoryItemResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        int theoreticalQuantitySnapshot,
        int reservedQuantitySnapshot,
        Integer countedQuantity,
        Integer theoreticalQuantityAtCount,
        Integer reservedQuantityAtCount,
        Integer differenceQuantity,
        OffsetDateTime countedAt,
        String countedBy,
        String countNote,
        Integer quantityBeforeApproval,
        Integer quantityAfterApproval,
        Integer reservedQuantityAtApproval,
        Integer compensatedMovementDelta,
        Long stockMovementId
) {
    static PhysicalInventoryItemResponse from(PhysicalInventoryItem item) {
        return new PhysicalInventoryItemResponse(
                item.getId(), item.getProductId(), item.getProductCodeSnapshot(), item.getProductNameSnapshot(),
                item.getTheoreticalQuantitySnapshot(), item.getReservedQuantitySnapshot(), item.getCountedQuantity(),
                item.getTheoreticalQuantityAtCount(), item.getReservedQuantityAtCount(), item.getDifferenceQuantity(),
                item.getCountedAt() == null ? null : BusinessTime.utcOffset(item.getCountedAt()), item.getCountedBy(),
                item.getCountNote(), item.getQuantityBeforeApproval(), item.getQuantityAfterApproval(),
                item.getReservedQuantityAtApproval(), item.getCompensatedMovementDelta(), item.getStockMovementId()
        );
    }
}
