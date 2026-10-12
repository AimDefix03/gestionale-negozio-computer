package it.giovannidefilippo.gestionale.user;

public enum PasswordStrength {
    WEAK("Debole"),
    MEDIUM("Media"),
    STRONG("Forte");

    private final String label;

    PasswordStrength(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
