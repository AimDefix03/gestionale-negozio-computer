package it.giovannidefilippo.gestionale.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class OrderOperationRequests {
    private OrderOperationRequests() {
    }

    public record ReceiptRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Size(max = 120) String reference,
            @NotBlank @Size(max = 500) String reason
    ) {
    }

    public record ReturnRequest(
            @NotBlank @Size(max = 500) String reason,
            @NotEmpty List<@Valid ReturnItemRequest> items
    ) {
    }

    public record ReturnItemRequest(@NotBlank String productCode, @Positive int quantity) {
    }

    public record ReturnReviewRequest(@Size(max = 500) String note) {
    }

    public record ReturnRejectionRequest(@NotBlank @Size(max = 500) String note) {
    }

    public record ReturnRefundRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Size(max = 120) String reference,
            @NotBlank @Size(max = 500) String reason
    ) {
    }
}
