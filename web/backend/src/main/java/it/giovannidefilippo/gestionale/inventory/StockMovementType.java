package it.giovannidefilippo.gestionale.inventory;

public enum StockMovementType {
    INITIAL_BALANCE("Saldo iniziale"),
    ADJUSTMENT_INCREASE("Rettifica positiva"),
    ADJUSTMENT_DECREASE("Rettifica negativa"),
    LOAD("Carico"),
    UNLOAD("Scarico"),
    RETURN("Reso cliente"),
    PURCHASE_RECEIPT("Ricezione fornitore"),
    PHYSICAL_INVENTORY_INCREASE("Inventario fisico positivo"),
    PHYSICAL_INVENTORY_DECREASE("Inventario fisico negativo");

    private final String label;

    StockMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
