package it.giovannidefilippo.gestionale.partner;

import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.time.OffsetDateTime;

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
        Long linkedAccountId,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
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
                partner.getLinkedAccountId(),
                partner.isActive(),
                BusinessTime.utcOffset(partner.getCreatedAt()),
                BusinessTime.utcOffset(partner.getUpdatedAt())
        );
    }
}
