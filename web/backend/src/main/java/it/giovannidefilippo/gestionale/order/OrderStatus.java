package it.giovannidefilippo.gestionale.order;

public enum OrderStatus {
    DRAFT("Bozza"),
    CONFIRMED("Confermato"),
    FULFILLED("Evaso"),
    CANCELED("Annullato");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
