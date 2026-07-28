package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.company.CompanySettingsSnapshot;

record CompanySnapshot(
        String legalName,
        String taxCode,
        String vatNumber,
        String email,
        String phone,
        String address,
        String postalCode,
        String city,
        String province,
        String countryCode
) {
    static CompanySnapshot from(CompanySettingsSnapshot settings) {
        return new CompanySnapshot(
                settings.legalName(), settings.taxCode(), settings.vatNumber(), settings.email(), settings.phone(),
                settings.address(), settings.postalCode(), settings.city(), settings.province(), settings.countryCode()
        );
    }

    static CompanySnapshot from(FiscalDocument document) {
        return new CompanySnapshot(
                document.getCompanySnapshotLegalName(), document.getCompanySnapshotTaxCode(), document.getCompanySnapshotVatNumber(),
                document.getCompanySnapshotEmail(), document.getCompanySnapshotPhone(), document.getCompanySnapshotAddress(),
                document.getCompanySnapshotPostalCode(), document.getCompanySnapshotCity(), document.getCompanySnapshotProvince(),
                document.getCompanySnapshotCountryCode()
        );
    }
}
