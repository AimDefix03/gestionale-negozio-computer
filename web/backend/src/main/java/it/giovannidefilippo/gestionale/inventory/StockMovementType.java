package it.giovannidefilippo.gestionale.inventory;

public enum StockMovementType {
    LOAD("Carico"),
    UNLOAD("Scarico"),
    RETURN("Reso cliente");

    private final String label;

    StockMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
