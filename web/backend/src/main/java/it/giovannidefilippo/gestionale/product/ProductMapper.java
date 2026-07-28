package it.giovannidefilippo.gestionale.product;

final class ProductMapper {
    private ProductMapper() {
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getBrand(),
                product.getProductType(),
                product.getUsageContext(),
                product.getQuantity(),
                product.getReservedQuantity(),
                product.getAvailableQuantity(),
                product.getPrice(),
                product.getDiscount(),
                product.getDiscountedPrice(),
                product.isDiscontinued()
        );
    }
}
