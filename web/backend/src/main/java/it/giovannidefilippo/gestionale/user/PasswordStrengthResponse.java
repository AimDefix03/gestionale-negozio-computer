package it.giovannidefilippo.gestionale.user;

import java.util.List;

public record PasswordStrengthResponse(PasswordStrength strength, String label, List<String> suggestions) {
}
