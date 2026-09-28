package it.giovannidefilippo.gestionale.user;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
class PasswordStrengthService {
    PasswordStrengthResponse evaluate(String password) {
        List<String> suggestions = new ArrayList<>();
        if (password == null || password.length() < 8) {
            suggestions.add("Usa almeno 8 caratteri.");
        }
        if (password == null || !password.matches(".*[A-Z].*")) {
            suggestions.add("Aggiungi una lettera maiuscola.");
        }
        if (password == null || !password.matches(".*[a-z].*")) {
            suggestions.add("Aggiungi una lettera minuscola.");
        }
        if (password == null || !password.matches(".*[0-9].*")) {
            suggestions.add("Aggiungi un numero.");
        }
        if (password == null || !password.matches(".*[^A-Za-z0-9].*")) {
            suggestions.add("Aggiungi un simbolo.");
        }

        PasswordStrength strength = suggestions.isEmpty()
                ? PasswordStrength.STRONG
                : suggestions.size() <= 2 ? PasswordStrength.MEDIUM : PasswordStrength.WEAK;
        return new PasswordStrengthResponse(strength, strength.getLabel(), suggestions);
    }

    void validateStrongPassword(String username, String password) {
        PasswordStrengthResponse evaluation = evaluate(password);
        List<String> violations = new ArrayList<>(evaluation.suggestions());
        if (isSimilarToUsername(username, password)) {
            violations.add("Non usare una password troppo simile allo username.");
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("La password non rispetta la policy di sicurezza: " + String.join(" ", violations));
        }
    }

    private static boolean isSimilarToUsername(String username, String password) {
        String normalizedUsername = normalize(username);
        String normalizedPassword = normalize(password);
        return normalizedUsername.length() >= 4 && normalizedPassword.contains(normalizedUsername);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
