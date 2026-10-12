package it.giovannidefilippo.gestionale.document;

public record FiscalDocumentCapabilities(boolean canCreateCreditNote) {
    static FiscalDocumentCapabilities none() {
        return new FiscalDocumentCapabilities(false);
    }
}
