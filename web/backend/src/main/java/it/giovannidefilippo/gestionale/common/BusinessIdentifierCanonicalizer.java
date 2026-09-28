package it.giovannidefilippo.gestionale.common;

import java.util.Locale;

public final class BusinessIdentifierCanonicalizer {
    private BusinessIdentifierCanonicalizer() {
    }

    public static String display(String value) {
        return value == null ? "" : value.trim();
    }

    public static String canonical(String value) {
        return display(value).toLowerCase(Locale.ROOT);
    }
}
