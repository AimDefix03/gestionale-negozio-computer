package it.giovannidefilippo.gestionale.partner;

import java.time.LocalDateTime;

public record BusinessPartnerResponse(
        Long id,
        String code,
        BusinessPartnerType type,
        String typeLabel,
        String displayName,
        String taxCode,
        String vatNumber,
        String email,
        String phone,
        String address,
        String city,
        String notes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    static BusinessPartnerResponse from(BusinessPartner partner) {
        return new BusinessPartnerResponse(
                partner.getId(),
                partner.getCode(),
                partner.getType(),
                partner.getType().getLabel(),
                partner.getDisplayName(),
                partner.getTaxCode(),
                partner.getVatNumber(),
                partner.getEmail(),
                partner.getPhone(),
                partner.getAddress(),
                partner.getCity(),
                partner.getNotes(),
                partner.isActive(),
                partner.getCreatedAt(),
                partner.getUpdatedAt()
        );
    }
}
