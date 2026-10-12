package it.giovannidefilippo.gestionale.order;

public enum OrderOwnershipStatus {
    ACCOUNT("Account collegato"),
    PARTNER("Anagrafica collegata"),
    UNRESOLVED("Da riconciliare");

    private final String label;

    OrderOwnershipStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
