package it.giovannidefilippo.gestionale.common;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Arrays;

public final class DatabaseConstraintViolations {
    private DatabaseConstraintViolations() {
    }

    public static boolean matches(Throwable throwable, String... constraintNames) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && matchesName(violation.getConstraintName(), constraintNames)) {
                return true;
            }
            if (matchesName(current.getMessage(), constraintNames)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean matchesName(String value, String... constraintNames) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return Arrays.stream(constraintNames)
                .map(name -> name.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(normalized::contains);
    }
}
