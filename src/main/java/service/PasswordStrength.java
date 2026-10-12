package service;

public enum PasswordStrength {
    EMPTY("Inserisci una password", 0),
    WEAK("Debole", 25),
    MEDIUM("Media", 55),
    STRONG("Forte", 80),
    EXCELLENT("Molto forte", 100);

    private final String label;
    private final int score;

    PasswordStrength(String label, int score) {
        this.label = label;
        this.score = score;
    }

    public String getLabel() {
        return label;
    }

    public int getScore() {
        return score;
    }
}
