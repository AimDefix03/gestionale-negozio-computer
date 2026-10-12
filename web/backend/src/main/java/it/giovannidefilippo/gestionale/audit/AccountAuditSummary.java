package it.giovannidefilippo.gestionale.audit;

public record AccountAuditSummary(
        long inventoryActions,
        long orderActions,
        long paymentActions,
        long returnActions,
        long documentActions
) {
}
