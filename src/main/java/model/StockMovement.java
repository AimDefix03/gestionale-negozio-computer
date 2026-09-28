package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record StockMovement(
        LocalDateTime timestamp,
        String actor,
        String role,
        String productCode,
        String productName,
        StockMovementType type,
        int quantity,
        int previousQuantity,
        int newQuantity,
        String reason
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
