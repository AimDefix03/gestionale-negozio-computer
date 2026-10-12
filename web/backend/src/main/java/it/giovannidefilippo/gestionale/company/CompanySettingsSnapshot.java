package it.giovannidefilippo.gestionale.company;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record CompanySettingsSnapshot(
        String legalName,
        String taxCode,
        String vatNumber,
        String email,
        String phone,
        String address,
        String postalCode,
        String city,
        String province,
        String countryCode,
        String timeZone,
        BigDecimal defaultVatRate,
        String invoicePrefix,
        String creditNotePrefix,
        int numberPadding
) {
    public List<String> missingDocumentFields() {
        List<String> missing = new ArrayList<>();
        requireText(legalName, "legalName", missing);
        if (!hasText(taxCode) && !hasText(vatNumber)) {
            missing.add("taxIdentifier");
        }
        requireText(address, "address", missing);
        requireText(postalCode, "postalCode", missing);
        requireText(city, "city", missing);
        requireText(province, "province", missing);
        requireText(countryCode, "countryCode", missing);
        requireText(timeZone, "timeZone", missing);
        return List.copyOf(missing);
    }

    public boolean isDocumentReady() {
        return missingDocumentFields().isEmpty();
    }

    public void requireDocumentReady() {
        List<String> missing = missingDocumentFields();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Completa i dati aziendali obbligatori prima di generare documenti: " + String.join(", ", missing) + ".");
        }
    }

    private static void requireText(String value, String field, List<String> missing) {
        if (!hasText(value)) {
            missing.add(field);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
