package service;

public final class PasswordStrengthEvaluator {
    private PasswordStrengthEvaluator() {
    }

    public static PasswordStrength evaluate(String password) {
        if (password == null || password.isBlank()) {
            return PasswordStrength.EMPTY;
        }

        int score = 0;
        if (password.length() >= 8) {
            score++;
        }
        if (password.length() >= 12) {
            score++;
        }
        if (password.chars().anyMatch(Character::isUpperCase)) {
            score++;
        }
        if (password.chars().anyMatch(Character::isLowerCase)) {
            score++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            score++;
        }
        if (password.chars().anyMatch(character -> !Character.isLetterOrDigit(character))) {
            score++;
        }

        if (score <= 2) {
            return PasswordStrength.WEAK;
        }
        if (score <= 4) {
            return PasswordStrength.MEDIUM;
        }
        if (score == 5) {
            return PasswordStrength.STRONG;
        }
        return PasswordStrength.EXCELLENT;
    }
}
