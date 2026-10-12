package it.giovannidefilippo.gestionale.document;

public enum FiscalDocumentType {
    SIMULATED_INVOICE("Fattura simulata", "FS"),
    SIMULATED_CREDIT_NOTE("Nota credito simulata", "NC");

    private final String label;
    private final String prefix;

    FiscalDocumentType(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    public String getLabel() { return label; }
    public String getPrefix() { return prefix; }
}
