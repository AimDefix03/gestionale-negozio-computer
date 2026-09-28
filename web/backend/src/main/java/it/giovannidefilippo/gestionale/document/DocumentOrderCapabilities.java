package it.giovannidefilippo.gestionale.document;

public record DocumentOrderCapabilities(
        boolean canCreateInvoice,
        boolean canCreateCreditNote,
        boolean hasInvoice,
        boolean hasCreditNote
) {
}
