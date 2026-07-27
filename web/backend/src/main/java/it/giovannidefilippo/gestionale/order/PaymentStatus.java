package it.giovannidefilippo.gestionale.order;

public enum PaymentStatus {
    PENDING("In attesa"),
    PARTIALLY_PAID("Parzialmente pagato"),
    PAID("Pagato"),
    FAILED("Non riuscito"),
    CANCELED("Annullato"),
    PARTIALLY_REFUNDED("Parzialmente rimborsato"),
    REFUNDED("Rimborsato");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
