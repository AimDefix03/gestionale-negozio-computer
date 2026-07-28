package it.giovannidefilippo.gestionale.partner;

public enum BusinessPartnerType {
    CUSTOMER("Cliente"),
    SUPPLIER("Fornitore");

    private final String label;

    BusinessPartnerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
