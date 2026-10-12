package it.giovannidefilippo.gestionale.company;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import it.giovannidefilippo.gestionale.common.BusinessTime;

public record CompanySettingsResponse(
        long version,
        boolean configured,
        List<String> missingDocumentFields,
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
        int numberPadding,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    static CompanySettingsResponse from(CompanySettings settings) {
        return new CompanySettingsResponse(
                settings.getVersion(), settings.isConfigured(), settings.snapshot().missingDocumentFields(), settings.getLegalName(), settings.getTaxCode(),
                settings.getVatNumber(), settings.getEmail(), settings.getPhone(), settings.getAddress(),
                settings.getPostalCode(), settings.getCity(), settings.getProvince(), settings.getCountryCode(), settings.getTimeZone(),
                settings.getDefaultVatRate(), settings.getInvoicePrefix(), settings.getCreditNotePrefix(),
                settings.getNumberPadding(), BusinessTime.offsetFromUtc(settings.getUpdatedAt(), settings.getTimeZone()), settings.getUpdatedBy()
        );
    }
}
