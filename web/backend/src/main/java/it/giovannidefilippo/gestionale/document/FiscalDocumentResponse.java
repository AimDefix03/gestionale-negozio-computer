package it.giovannidefilippo.gestionale.document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import it.giovannidefilippo.gestionale.common.BusinessTime;

public record FiscalDocumentResponse(
        Long id,
        String code,
        int fiscalYear,
        long sequenceNumber,
        String documentPrefix,
        FiscalDocumentType type,
        String typeLabel,
        FiscalDocumentStatus status,
        String statusLabel,
        OffsetDateTime createdAt,
        String relatedOrderCode,
        String customer,
        String companySnapshotLegalName,
        String companySnapshotTaxCode,
        String companySnapshotVatNumber,
        String companySnapshotEmail,
        String companySnapshotPhone,
        String companySnapshotAddress,
        String companySnapshotPostalCode,
        String companySnapshotCity,
        String companySnapshotProvince,
        String companySnapshotCountryCode,
        String companySnapshotTimeZone,
        String customerSnapshotCode,
        String customerSnapshotName,
        String customerSnapshotTaxCode,
        String customerSnapshotVatNumber,
        String customerSnapshotEmail,
        String customerSnapshotPhone,
        String customerSnapshotAddress,
        String customerSnapshotCity,
        String paymentMethod,
        List<LineResponse> lines,
        BigDecimal taxableAmount,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        String createdBy,
        String createdByRole,
        String reason,
        String disclaimer,
        FiscalDocumentCapabilities capabilities
) {
    static FiscalDocumentResponse from(FiscalDocument document) {
        return from(document, FiscalDocumentCapabilities.none());
    }

    static FiscalDocumentResponse from(FiscalDocument document, FiscalDocumentCapabilities capabilities) {
        return new FiscalDocumentResponse(
                document.getId(),
                document.getCode(),
                document.getFiscalYear(),
                document.getSequenceNumber(),
                document.getDocumentPrefix(),
                document.getType(),
                document.getType().getLabel(),
                document.getStatus(),
                document.getStatus().getLabel(),
                BusinessTime.offsetFromUtc(document.getCreatedAt(), document.getCompanySnapshotTimeZone()),
                document.getRelatedOrderCode(),
                document.getCustomer(),
                document.getCompanySnapshotLegalName(),
                document.getCompanySnapshotTaxCode(),
                document.getCompanySnapshotVatNumber(),
                document.getCompanySnapshotEmail(),
                document.getCompanySnapshotPhone(),
                document.getCompanySnapshotAddress(),
                document.getCompanySnapshotPostalCode(),
                document.getCompanySnapshotCity(),
                document.getCompanySnapshotProvince(),
                document.getCompanySnapshotCountryCode(),
                document.getCompanySnapshotTimeZone(),
                document.getCustomerSnapshotCode(),
                document.getCustomerSnapshotName(),
                document.getCustomerSnapshotTaxCode(),
                document.getCustomerSnapshotVatNumber(),
                document.getCustomerSnapshotEmail(),
                document.getCustomerSnapshotPhone(),
                document.getCustomerSnapshotAddress(),
                document.getCustomerSnapshotCity(),
                document.getPaymentMethod(),
                document.getLines().stream().map(LineResponse::from).toList(),
                document.getTaxableAmount(),
                document.getVatRate(),
                document.getVatAmount(),
                document.getTotalAmount(),
                document.getCreatedBy(),
                document.getCreatedByRole(),
                document.getReason(),
                document.getDisclaimer(),
                capabilities
        );
    }

    public record LineResponse(String productCode, String description, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static LineResponse from(FiscalDocumentLine line) {
            return new LineResponse(line.getProductCode(), line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getLineTotal());
        }
    }
}
