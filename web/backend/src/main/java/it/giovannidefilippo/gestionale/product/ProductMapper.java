package it.giovannidefilippo.gestionale.product;

final class ProductMapper {
    private ProductMapper() {
    }

    static ProductResponse toResponse(Product product) {
        return toResponse(product, ProductCapabilities.none());
    }

    static ProductResponse toResponse(Product product, ProductCapabilities capabilities) {
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
                product.getLastPurchaseCost(),
                product.getAveragePurchaseCost(),
                product.getCostedQuantity(),
                product.getUncostedQuantity(),
                product.getCostCoveragePercentage(),
                product.getKnownInventoryCost(),
                product.getPotentialGrossMarginOnCostedStock(),
                product.isDiscontinued(),
                capabilities
        );
    }
}
