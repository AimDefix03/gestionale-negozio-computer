package it.giovannidefilippo.gestionale.product;

import java.math.BigDecimal;

public record CustomerProductResponse(
        String code,
        String name,
        String description,
        ProductCategory category,
        String brand,
        String productType,
        String usageContext,
        BigDecimal price,
        BigDecimal discount,
        BigDecimal discountedPrice,
        CommercialAvailability availability,
        String availabilityLabel
) {
    private static final int LIMITED_AVAILABILITY_THRESHOLD = 3;

    static CustomerProductResponse from(Product product) {
        CommercialAvailability availability = availability(product.getAvailableQuantity());
        return new CustomerProductResponse(
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getBrand(),
                product.getProductType(),
                product.getUsageContext(),
                product.getPrice(),
                product.getDiscount(),
                product.getDiscountedPrice(),
                availability,
                availability.getLabel()
        );
    }

    private static CommercialAvailability availability(int availableQuantity) {
        if (availableQuantity <= 0) {
            return CommercialAvailability.UNAVAILABLE;
        }
        if (availableQuantity <= LIMITED_AVAILABILITY_THRESHOLD) {
            return CommercialAvailability.LIMITED;
        }
        return CommercialAvailability.AVAILABLE;
    }
}
