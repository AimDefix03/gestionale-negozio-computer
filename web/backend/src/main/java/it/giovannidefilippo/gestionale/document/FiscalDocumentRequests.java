package it.giovannidefilippo.gestionale.document;

import jakarta.validation.constraints.NotBlank;

public final class FiscalDocumentRequests {
    private FiscalDocumentRequests() {
    }

    public record CreateInvoiceRequest(@NotBlank String orderCode) {
    }

    public record CreateCreditNoteRequest(@NotBlank String orderCode, @NotBlank String reason) {
    }
}
