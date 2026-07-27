package it.giovannidefilippo.gestionale.order;

public enum PaymentTransactionType {
    RECEIPT("Incasso"),
    REFUND("Rimborso");

    private final String label;

    PaymentTransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
