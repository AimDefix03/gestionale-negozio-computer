package it.giovannidefilippo.gestionale.purchase;

public enum SupplierOrderStatus {
    DRAFT("Bozza"),
    SENT("Inviato"),
    PARTIALLY_RECEIVED("Ricevuto parzialmente"),
    RECEIVED("Ricevuto"),
    CANCELED("Annullato");

    private final String label;

    SupplierOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
