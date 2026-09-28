package it.giovannidefilippo.gestionale.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "document_number_counters")
@IdClass(DocumentNumberCounterId.class)
class DocumentNumberCounter {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private FiscalDocumentType type;

    @Id
    @Column(nullable = false)
    private Integer fiscalYear;

    @Column(nullable = false)
    private long nextValue;

    @Version
    private long version;

    protected DocumentNumberCounter() {
    }

    DocumentNumberCounter(FiscalDocumentType type, int fiscalYear) {
        this.type = type;
        this.fiscalYear = fiscalYear;
        this.nextValue = 1;
    }

    long takeNext() {
        return nextValue++;
    }
}
