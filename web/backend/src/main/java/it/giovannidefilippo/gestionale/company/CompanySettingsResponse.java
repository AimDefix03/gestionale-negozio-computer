package it.giovannidefilippo.gestionale.company;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanySettingsResponse(
        long version,
        boolean configured,
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
        int numberPadding,
        LocalDateTime updatedAt,
        String updatedBy
) {
    static CompanySettingsResponse from(CompanySettings settings) {
        return new CompanySettingsResponse(
                settings.getVersion(), settings.isConfigured(), settings.getLegalName(), settings.getTaxCode(),
                settings.getVatNumber(), settings.getEmail(), settings.getPhone(), settings.getAddress(),
                settings.getPostalCode(), settings.getCity(), settings.getProvince(), settings.getCountryCode(),
                settings.getDefaultVatRate(), settings.getInvoicePrefix(), settings.getCreditNotePrefix(),
                settings.getNumberPadding(), settings.getUpdatedAt(), settings.getUpdatedBy()
        );
    }
}
