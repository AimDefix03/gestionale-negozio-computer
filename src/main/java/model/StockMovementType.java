package model;

public enum StockMovementType {
    CARICO("Carico"),
    SCARICO("Scarico");

    private final String label;

    StockMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static StockMovementType fromLabel(String label) {
        for (StockMovementType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo movimento non valido.");
    }
}
