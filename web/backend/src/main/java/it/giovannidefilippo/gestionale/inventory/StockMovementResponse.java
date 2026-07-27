package it.giovannidefilippo.gestionale.inventory;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        LocalDateTime timestamp,
        String actor,
        String role,
        String productCode,
        String productName,
        StockMovementType type,
        String typeLabel,
        int quantity,
        int previousQuantity,
        int newQuantity,
        String reason
) {
    static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getTimestamp(),
                movement.getActor(),
                movement.getRole(),
                movement.getProductCode(),
                movement.getProductName(),
                movement.getType(),
                movement.getType().getLabel(),
                movement.getQuantity(),
                movement.getPreviousQuantity(),
                movement.getNewQuantity(),
                movement.getReason()
        );
    }
}
