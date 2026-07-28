package model;

import java.io.Serializable;

public record FiscalDocumentLine(
        String productCode,
        String description,
        int quantity,
        double unitPrice,
        double lineTotal
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
