package it.giovannidefilippo.gestionale.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SupplierOrderRequests {
    private SupplierOrderRequests() {
    }

    public record CreateRequest(
            @Positive long supplierId,
            @NotNull @FutureOrPresent LocalDate expectedDeliveryDate,
            @Size(max = 1200) String notes,
            @NotEmpty List<@Valid CreateItemRequest> items
    ) {
    }

    public record CreateItemRequest(
            @NotBlank String productCode,
            @Positive int quantity,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            @FutureOrPresent LocalDate expectedDeliveryDate
    ) {
    }

    public record ReceiveRequest(
            @NotBlank @Size(max = 900) String reason,
            @NotEmpty List<@Valid ReceiveItemRequest> items
    ) {
    }

    public record ReceiveItemRequest(
            @Positive long lineId,
            @Positive int quantity,
            @DecimalMin("0.00") BigDecimal unitCost
    ) {
        public ReceiveItemRequest(long lineId, int quantity) {
            this(lineId, quantity, null);
        }
    }

    public record CancelRequest(@NotBlank @Size(max = 900) String reason) {
    }
}
