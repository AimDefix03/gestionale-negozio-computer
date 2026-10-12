package it.giovannidefilippo.gestionale.product;

public enum ProductCategory {
    HARDWARE("Hardware"),
    SOFTWARE("Software");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
