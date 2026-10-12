package it.giovannidefilippo.gestionale.inventory;

public enum InventoryReconciliationStatus {
    BALANCED("Riconciliato"),
    MISSING_INITIAL_BALANCE("Saldo iniziale assente"),
    UNVERIFIED_INITIAL_BALANCE("Saldo storico da verificare"),
    CHAIN_BROKEN("Catena movimenti non coerente"),
    LEDGER_DRIFT("Giacenza diversa dal ledger");

    private final String label;

    InventoryReconciliationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
