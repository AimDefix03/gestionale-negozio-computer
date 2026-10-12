package it.giovannidefilippo.gestionale.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

final class PhysicalInventoryRequests {
    private PhysicalInventoryRequests() {
    }

    record Create(
            @NotBlank @Size(max = 900) String reason,
            @Size(max = 500) List<@NotBlank String> productCodes
    ) {
    }

    record Count(
            @PositiveOrZero int countedQuantity,
            @Size(max = 900) String note
    ) {
    }

    record Decision(@NotBlank @Size(max = 900) String reason) {
    }
}
