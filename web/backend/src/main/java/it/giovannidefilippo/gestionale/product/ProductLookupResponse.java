package it.giovannidefilippo.gestionale.product;

public record ProductLookupResponse(
        String code,
        String name,
        String brand,
        String productType,
        boolean discontinued,
        int availableQuantity
) {
    static ProductLookupResponse from(Product product) {
        return new ProductLookupResponse(
                product.getCode(),
                product.getName(),
                product.getBrand(),
                product.getProductType(),
                product.isDiscontinued(),
                product.getAvailableQuantity()
        );
    }
}
