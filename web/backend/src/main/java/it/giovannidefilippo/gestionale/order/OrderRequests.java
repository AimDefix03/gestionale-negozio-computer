package it.giovannidefilippo.gestionale.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class OrderRequests {
    private OrderRequests() {
    }

    public record CreateOrderRequest(
            @NotBlank String customer,
            String customerCode,
            @NotNull PaymentMethod paymentMethod,
            @NotEmpty List<@Valid CreateOrderItemRequest> items
    ) {
        public CreateOrderRequest(String customer, PaymentMethod paymentMethod, List<@Valid CreateOrderItemRequest> items) {
            this(customer, null, paymentMethod, items);
        }
    }

    public record CreateOrderItemRequest(@NotBlank String productCode, @Positive int quantity) {
    }
}
