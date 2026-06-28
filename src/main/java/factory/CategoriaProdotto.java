package factory;

public enum CategoriaProdotto {
    HARDWARE("Hardware"),
    SOFTWARE("Software");

    private final String label;

    CategoriaProdotto(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static CategoriaProdotto fromLabel(String label) {
        for (CategoriaProdotto categoria : values()) {
            if (categoria.label.equalsIgnoreCase(label)) {
                return categoria;
            }
        }
        throw new IllegalArgumentException("Categoria prodotto non supportata: " + label);
    }
}
