package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

public record StockMovementResponse(
        Long id,
        OffsetDateTime timestamp,
        String actor,
        String role,
        String productCode,
        String productName,
        Long productId,
        StockMovementType type,
        String typeLabel,
        int quantity,
        int previousQuantity,
        int newQuantity,
        int deltaQuantity,
        StockMovementOrigin origin,
        boolean authoritative,
        Long supplierOrderId,
        Long supplierOrderReceiptId,
        Long supplierOrderReceiptItemId,
        BigDecimal unitCost,
        BigDecimal totalCost,
        BigDecimal averageCostBefore,
        BigDecimal averageCostAfter,
        Integer costedQuantityBefore,
        Integer costedQuantityAfter,
        Long physicalInventorySessionId,
        Long physicalInventoryItemId,
        String reason
) {
    static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                BusinessTime.utcOffset(movement.getTimestamp()),
                movement.getActor(),
                movement.getRole(),
                movement.getProductCode(),
                movement.getProductName(),
                movement.getProductId(),
                movement.getType(),
                movement.getType().getLabel(),
                movement.getQuantity(),
                movement.getPreviousQuantity(),
                movement.getNewQuantity(),
                movement.getDeltaQuantity(),
                movement.getOrigin(),
                movement.isAuthoritative(),
                movement.getSupplierOrderId(),
                movement.getSupplierOrderReceiptId(),
                movement.getSupplierOrderReceiptItemId(),
                movement.getUnitCost(),
                movement.getTotalCost(),
                movement.getAverageCostBefore(),
                movement.getAverageCostAfter(),
                movement.getCostedQuantityBefore(),
                movement.getCostedQuantityAfter(),
                movement.getPhysicalInventorySessionId(),
                movement.getPhysicalInventoryItemId(),
                movement.getReason()
        );
    }
}
