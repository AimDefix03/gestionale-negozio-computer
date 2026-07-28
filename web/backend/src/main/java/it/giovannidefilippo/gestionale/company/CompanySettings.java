package it.giovannidefilippo.gestionale.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_settings")
class CompanySettings {
    static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(nullable = false, length = 160)
    private String legalName;

    @Column(nullable = false, length = 32)
    private String taxCode;

    @Column(nullable = false, length = 32)
    private String vatNumber;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 16)
    private String postalCode;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 8)
    private String province;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal defaultVatRate;

    @Column(nullable = false, length = 8)
    private String invoicePrefix;

    @Column(nullable = false, length = 8)
    private String creditNotePrefix;

    @Column(nullable = false)
    private int numberPadding;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String updatedBy;

    @Version
    private long version;

    protected CompanySettings() {
    }

    void update(CompanySettingsRequests.UpdateRequest request, String actor, LocalDateTime timestamp) {
        legalName = optional(request.legalName());
        taxCode = optional(request.taxCode());
        vatNumber = optional(request.vatNumber());
        email = optional(request.email());
        phone = optional(request.phone());
        address = optional(request.address());
        postalCode = optional(request.postalCode());
        city = optional(request.city());
        province = optional(request.province()).toUpperCase();
        countryCode = optional(request.countryCode()).toUpperCase();
        defaultVatRate = request.defaultVatRate().stripTrailingZeros();
        invoicePrefix = request.invoicePrefix().trim().toUpperCase();
        creditNotePrefix = request.creditNotePrefix().trim().toUpperCase();
        numberPadding = request.numberPadding();
        updatedAt = timestamp;
        updatedBy = actor;
    }

    CompanySettingsSnapshot snapshot() {
        return new CompanySettingsSnapshot(
                legalName, taxCode, vatNumber, email, phone, address, postalCode, city, province,
                countryCode, defaultVatRate, invoicePrefix, creditNotePrefix, numberPadding
        );
    }

    boolean isConfigured() {
        return !legalName.isBlank() && (!taxCode.isBlank() || !vatNumber.isBlank());
    }

    Integer getId() { return id; }
    String getLegalName() { return legalName; }
    String getTaxCode() { return taxCode; }
    String getVatNumber() { return vatNumber; }
    String getEmail() { return email; }
    String getPhone() { return phone; }
    String getAddress() { return address; }
    String getPostalCode() { return postalCode; }
    String getCity() { return city; }
    String getProvince() { return province; }
    String getCountryCode() { return countryCode; }
    BigDecimal getDefaultVatRate() { return defaultVatRate; }
    String getInvoicePrefix() { return invoicePrefix; }
    String getCreditNotePrefix() { return creditNotePrefix; }
    int getNumberPadding() { return numberPadding; }
    LocalDateTime getUpdatedAt() { return updatedAt; }
    String getUpdatedBy() { return updatedBy; }
    long getVersion() { return version; }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
