package it.giovannidefilippo.gestionale.product;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeleteProductsRequest(@NotEmpty List<String> codes) {
}
