package it.giovannidefilippo.gestionale.order;

public enum OrderCustomerType {
    SELF_SERVICE("Cliente autenticato"),
    REGISTERED("Cliente censito"),
    WALK_IN("Cliente occasionale"),
    LEGACY_UNRESOLVED("Storico non classificato");

    private final String label;

    OrderCustomerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
