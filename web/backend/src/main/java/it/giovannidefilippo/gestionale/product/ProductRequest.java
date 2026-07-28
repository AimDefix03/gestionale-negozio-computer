package it.giovannidefilippo.gestionale.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull ProductCategory category,
        @NotBlank String brand,
        @NotBlank String productType,
        String usageContext,
        @PositiveOrZero int quantity,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discount
) {
}
