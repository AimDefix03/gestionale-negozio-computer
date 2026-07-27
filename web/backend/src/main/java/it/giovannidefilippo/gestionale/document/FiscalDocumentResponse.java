package it.giovannidefilippo.gestionale.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime createdAt,
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
        String disclaimer
) {
    static FiscalDocumentResponse from(FiscalDocument document) {
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
                document.getCreatedAt(),
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
                document.getDisclaimer()
        );
    }

    public record LineResponse(String productCode, String description, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static LineResponse from(FiscalDocumentLine line) {
            return new LineResponse(line.getProductCode(), line.getDescription(), line.getQuantity(), line.getUnitPrice(), line.getLineTotal());
        }
    }
}
