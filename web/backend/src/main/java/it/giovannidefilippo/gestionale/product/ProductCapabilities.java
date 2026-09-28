package it.giovannidefilippo.gestionale.product;

public record ProductCapabilities(
        boolean canEdit,
        boolean canChangeCode,
        boolean canDelete,
        boolean canDiscontinue,
        boolean canMoveStock
) {
    static ProductCapabilities none() {
        return new ProductCapabilities(false, false, false, false, false);
    }
}
