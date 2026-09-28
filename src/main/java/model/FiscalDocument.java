package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record FiscalDocument(
        String code,
        FiscalDocumentType type,
        FiscalDocumentStatus status,
        LocalDateTime createdAt,
        String relatedOrderCode,
        String customer,
        String paymentMethod,
        List<FiscalDocumentLine> lines,
        double taxableAmount,
        double vatRate,
        double vatAmount,
        double totalAmount,
        String createdBy,
        String createdByRole,
        String reason,
        String disclaimer
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
