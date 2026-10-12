package it.giovannidefilippo.gestionale.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryAdjustmentRequest(
        @NotBlank String productCode,
        @NotNull Integer delta,
        @NotBlank String reason
) {
}
