package it.giovannidefilippo.gestionale.document;

import java.io.Serializable;
import java.util.Objects;

public final class DocumentNumberCounterId implements Serializable {
    private FiscalDocumentType type;
    private Integer fiscalYear;

    public DocumentNumberCounterId() {
    }

    DocumentNumberCounterId(FiscalDocumentType type, Integer fiscalYear) {
        this.type = type;
        this.fiscalYear = fiscalYear;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DocumentNumberCounterId that)) return false;
        return type == that.type && Objects.equals(fiscalYear, that.fiscalYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, fiscalYear);
    }
}
