package it.giovannidefilippo.gestionale.order;

public enum PaymentTransactionType {
    RECEIPT("Incasso"),
    REFUND("Rimborso"),
    REVERSAL("Storno annullamento"),
    RECONCILIATION("Riconciliazione storica");

    private final String label;

    PaymentTransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
