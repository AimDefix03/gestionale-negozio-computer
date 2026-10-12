package it.giovannidefilippo.gestionale.partner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BusinessPartnerRequest(
        @NotBlank String code,
        @NotNull BusinessPartnerType type,
        @NotBlank String displayName,
        String taxCode,
        String vatNumber,
        String email,
        String phone,
        @Size(max = 600) String address,
        String city,
        @Size(max = 1200) String notes
) {
}
