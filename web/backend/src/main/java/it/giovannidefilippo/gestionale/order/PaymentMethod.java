package it.giovannidefilippo.gestionale.order;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum PaymentMethod {
    CARD("Carta"),
    BANK_TRANSFER("Bonifico bancario"),
    CASH("Contanti"),
    OTHER("Altro");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isSelectable() {
        return this != OTHER;
    }

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Il metodo di pagamento e obbligatorio.");
        }
        return switch (normalize(value)) {
            case "CARD", "CARTA" -> CARD;
            case "BANK_TRANSFER", "BONIFICO", "BONIFICO_BANCARIO" -> BANK_TRANSFER;
            case "CASH", "CONTANTI" -> CASH;
            case "OTHER", "ALTRO" -> OTHER;
            default -> throw new IllegalArgumentException("Metodo di pagamento non supportato.");
        };
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
