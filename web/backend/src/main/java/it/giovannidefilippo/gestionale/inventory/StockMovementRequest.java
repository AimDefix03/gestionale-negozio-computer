package it.giovannidefilippo.gestionale.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockMovementRequest(
        @NotBlank String productCode,
        @NotNull StockMovementType type,
        @Positive int quantity,
        @NotBlank String reason
) {
}
