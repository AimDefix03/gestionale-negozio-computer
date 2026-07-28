package it.giovannidefilippo.gestionale.company;

import java.math.BigDecimal;

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
        BigDecimal defaultVatRate,
        String invoicePrefix,
        String creditNotePrefix,
        int numberPadding
) {
}
