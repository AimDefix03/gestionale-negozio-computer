package it.giovannidefilippo.gestionale.order;

public enum OrderReturnStatus {
    REQUESTED("Richiesto"),
    APPROVED("Approvato"),
    REJECTED("Rifiutato"),
    RECEIVED("Ricevuto"),
    PARTIALLY_REFUNDED("Parzialmente rimborsato"),
    REFUNDED("Rimborsato");

    private final String label;

    OrderReturnStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean reservesQuantity() {
        return this != REJECTED;
    }
}
