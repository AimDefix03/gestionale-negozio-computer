package it.giovannidefilippo.gestionale.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class OrderRequests {
    private OrderRequests() {
    }

    public record CreateOrderRequest(
            String customer,
            String customerCode,
            OrderCustomerType customerType,
            @Positive Long customerPartnerId,
            @Size(max = 255) String walkInCustomerName,
            @NotNull PaymentMethod paymentMethod,
            @NotEmpty List<@Valid CreateOrderItemRequest> items
    ) {
        public CreateOrderRequest(String customer, String customerCode, PaymentMethod paymentMethod, List<@Valid CreateOrderItemRequest> items) {
            this(customer, customerCode, null, null, null, paymentMethod, items);
        }

        public CreateOrderRequest(String customer, PaymentMethod paymentMethod, List<@Valid CreateOrderItemRequest> items) {
            this(customer, null, null, null, null, paymentMethod, items);
        }
    }

    public record CreateOrderItemRequest(@NotBlank String productCode, @Positive int quantity) {
    }
}
