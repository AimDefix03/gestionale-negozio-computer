package it.giovannidefilippo.gestionale.inventory;

public enum PhysicalInventoryStatus {
    OPEN("In conteggio"),
    SUBMITTED("In approvazione"),
    APPROVED("Approvata"),
    CANCELED("Annullata");

    private final String label;

    PhysicalInventoryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
