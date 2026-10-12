package it.giovannidefilippo.gestionale.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record InitialStockRequest(
        @NotBlank String productCode,
        @PositiveOrZero int quantity,
        @NotBlank String reason
) {
}
