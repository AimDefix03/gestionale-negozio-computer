package it.giovannidefilippo.gestionale.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fiscal_documents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_fiscal_documents_order_type", columnNames = {"related_order_code", "type"})
})
public class FiscalDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private int fiscalYear;

    @Column(nullable = false)
    private long sequenceNumber;

    @Column(nullable = false, length = 8)
    private String documentPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalDocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FiscalDocumentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String relatedOrderCode;

    @Column(nullable = false)
    private String customer;

    @Column(nullable = false, length = 160)
    private String companySnapshotLegalName;

    @Column(nullable = false, length = 32)
    private String companySnapshotTaxCode;

    @Column(nullable = false, length = 32)
    private String companySnapshotVatNumber;

    @Column(nullable = false, length = 160)
    private String companySnapshotEmail;

    @Column(nullable = false, length = 40)
    private String companySnapshotPhone;

    @Column(nullable = false, length = 300)
    private String companySnapshotAddress;

    @Column(nullable = false, length = 16)
    private String companySnapshotPostalCode;

    @Column(nullable = false, length = 120)
    private String companySnapshotCity;

    @Column(nullable = false, length = 8)
    private String companySnapshotProvince;

    @Column(nullable = false, length = 2)
    private String companySnapshotCountryCode;

    @Column(nullable = false)
    private String customerSnapshotCode;

    @Column(nullable = false)
    private String customerSnapshotName;

    @Column(nullable = false)
    private String customerSnapshotTaxCode;

    @Column(nullable = false)
    private String customerSnapshotVatNumber;

    @Column(nullable = false)
    private String customerSnapshotEmail;

    @Column(nullable = false)
    private String customerSnapshotPhone;

    @Column(nullable = false, length = 600)
    private String customerSnapshotAddress;

    @Column(nullable = false)
    private String customerSnapshotCity;

    @Column(nullable = false)
    private String paymentMethod;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<FiscalDocumentLine> lines = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxableAmount;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal vatRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String createdByRole;

    @Column(nullable = false, length = 900)
    private String reason;

    @Column(nullable = false)
    private String disclaimer;

    protected FiscalDocument() {
    }

    FiscalDocument(DocumentNumberAllocation number, FiscalDocumentType type, String relatedOrderCode, CompanySnapshot companySnapshot, CustomerSnapshot customerSnapshot, String paymentMethod, List<FiscalDocumentLine> lines, BigDecimal taxableAmount, BigDecimal vatRate, BigDecimal vatAmount, BigDecimal totalAmount, String createdBy, String createdByRole, String reason, String disclaimer, LocalDateTime createdAt) {
        this.code = number.code();
        this.fiscalYear = number.fiscalYear();
        this.sequenceNumber = number.sequenceNumber();
        this.documentPrefix = number.prefix();
        this.type = type;
        this.status = FiscalDocumentStatus.ISSUED;
        this.createdAt = createdAt;
        this.relatedOrderCode = relatedOrderCode;
        this.customer = customerSnapshot.name();
        this.companySnapshotLegalName = companySnapshot.legalName();
        this.companySnapshotTaxCode = companySnapshot.taxCode();
        this.companySnapshotVatNumber = companySnapshot.vatNumber();
        this.companySnapshotEmail = companySnapshot.email();
        this.companySnapshotPhone = companySnapshot.phone();
        this.companySnapshotAddress = companySnapshot.address();
        this.companySnapshotPostalCode = companySnapshot.postalCode();
        this.companySnapshotCity = companySnapshot.city();
        this.companySnapshotProvince = companySnapshot.province();
        this.companySnapshotCountryCode = companySnapshot.countryCode();
        this.customerSnapshotCode = customerSnapshot.code();
        this.customerSnapshotName = customerSnapshot.name();
        this.customerSnapshotTaxCode = customerSnapshot.taxCode();
        this.customerSnapshotVatNumber = customerSnapshot.vatNumber();
        this.customerSnapshotEmail = customerSnapshot.email();
        this.customerSnapshotPhone = customerSnapshot.phone();
        this.customerSnapshotAddress = customerSnapshot.address();
        this.customerSnapshotCity = customerSnapshot.city();
        this.paymentMethod = paymentMethod;
        this.taxableAmount = taxableAmount;
        this.vatRate = vatRate;
        this.vatAmount = vatAmount;
        this.totalAmount = totalAmount;
        this.createdBy = clean(createdBy);
        this.createdByRole = clean(createdByRole);
        this.reason = reason;
        this.disclaimer = disclaimer;
        lines.forEach(this::addLine);
    }

    private void addLine(FiscalDocumentLine line) {
        line.assignDocument(this);
        lines.add(line);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public int getFiscalYear() { return fiscalYear; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getDocumentPrefix() { return documentPrefix; }
    public FiscalDocumentType getType() { return type; }
    public FiscalDocumentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRelatedOrderCode() { return relatedOrderCode; }
    public String getCustomer() { return customer; }
    public String getCompanySnapshotLegalName() { return companySnapshotLegalName; }
    public String getCompanySnapshotTaxCode() { return companySnapshotTaxCode; }
    public String getCompanySnapshotVatNumber() { return companySnapshotVatNumber; }
    public String getCompanySnapshotEmail() { return companySnapshotEmail; }
    public String getCompanySnapshotPhone() { return companySnapshotPhone; }
    public String getCompanySnapshotAddress() { return companySnapshotAddress; }
    public String getCompanySnapshotPostalCode() { return companySnapshotPostalCode; }
    public String getCompanySnapshotCity() { return companySnapshotCity; }
    public String getCompanySnapshotProvince() { return companySnapshotProvince; }
    public String getCompanySnapshotCountryCode() { return companySnapshotCountryCode; }
    public String getCustomerSnapshotCode() { return customerSnapshotCode; }
    public String getCustomerSnapshotName() { return customerSnapshotName; }
    public String getCustomerSnapshotTaxCode() { return customerSnapshotTaxCode; }
    public String getCustomerSnapshotVatNumber() { return customerSnapshotVatNumber; }
    public String getCustomerSnapshotEmail() { return customerSnapshotEmail; }
    public String getCustomerSnapshotPhone() { return customerSnapshotPhone; }
    public String getCustomerSnapshotAddress() { return customerSnapshotAddress; }
    public String getCustomerSnapshotCity() { return customerSnapshotCity; }
    public String getPaymentMethod() { return paymentMethod; }
    public List<FiscalDocumentLine> getLines() { return List.copyOf(lines); }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public BigDecimal getVatRate() { return vatRate; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedByRole() { return createdByRole; }
    public String getReason() { return reason; }
    public String getDisclaimer() { return disclaimer; }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
