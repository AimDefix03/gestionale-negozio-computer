package it.giovannidefilippo.gestionale.partner;

import it.giovannidefilippo.gestionale.common.BusinessIdentifierCanonicalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_partners")
public class BusinessPartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, unique = true)
    private String codeCanonical;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessPartnerType type;

    @Column(nullable = false)
    private String displayName;

    private String taxCode;
    private String vatNumber;
    private String email;
    private String phone;

    @Column(length = 600)
    private String address;

    private String city;

    @Column(length = 1200)
    private String notes;

    private Long linkedAccountId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected BusinessPartner() {
    }

    BusinessPartner(BusinessPartnerRequest request, LocalDateTime now) {
        this.createdAt = now;
        this.active = true;
        update(request, now);
        this.updatedAt = now;
    }

    void update(BusinessPartnerRequest request, LocalDateTime updatedAt) {
        if (linkedAccountId != null && request.type() != BusinessPartnerType.CUSTOMER) {
            throw new IllegalStateException("Scollega l'account cliente prima di modificare il tipo di anagrafica.");
        }
        this.code = BusinessIdentifierCanonicalizer.display(request.code());
        this.codeCanonical = BusinessIdentifierCanonicalizer.canonical(request.code());
        this.type = request.type();
        this.displayName = clean(request.displayName());
        this.taxCode = optional(request.taxCode());
        this.vatNumber = optional(request.vatNumber());
        this.email = optional(request.email());
        this.phone = optional(request.phone());
        this.address = optional(request.address());
        this.city = optional(request.city());
        this.notes = optional(request.notes());
        this.updatedAt = updatedAt;
    }

    void deactivate(LocalDateTime updatedAt) {
        this.active = false;
        this.updatedAt = updatedAt;
    }

    void reactivate(LocalDateTime updatedAt) {
        this.active = true;
        this.updatedAt = updatedAt;
    }

    void linkAccount(long accountId, LocalDateTime updatedAt) {
        if (type != BusinessPartnerType.CUSTOMER) {
            throw new IllegalStateException("Puoi collegare un account soltanto a un'anagrafica cliente.");
        }
        this.linkedAccountId = accountId;
        this.updatedAt = updatedAt;
    }

    void unlinkAccount(LocalDateTime updatedAt) {
        this.linkedAccountId = null;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public BusinessPartnerType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public String getTaxCode() { return taxCode; }
    public String getVatNumber() { return vatNumber; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getNotes() { return notes; }
    public Long getLinkedAccountId() { return linkedAccountId; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    private static String clean(String value) {
        return value.trim();
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
