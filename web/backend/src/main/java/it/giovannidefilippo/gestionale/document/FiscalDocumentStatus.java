package it.giovannidefilippo.gestionale.document;

public enum FiscalDocumentStatus {
    DRAFT("Bozza"),
    ISSUED("Emesso"),
    RECTIFIED("Rettificato");

    private final String label;

    FiscalDocumentStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
