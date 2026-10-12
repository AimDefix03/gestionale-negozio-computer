package it.giovannidefilippo.gestionale.inventory;

public record PhysicalInventoryCapabilities(
        boolean canCount,
        boolean canSubmit,
        boolean canApprove,
        boolean canCancel
) {
}
