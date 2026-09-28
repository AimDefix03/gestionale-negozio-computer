package it.giovannidefilippo.gestionale.product;

public enum CommercialAvailability {
    AVAILABLE("Disponibile"),
    LIMITED("Disponibilita limitata"),
    UNAVAILABLE("Non disponibile");

    private final String label;

    CommercialAvailability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
