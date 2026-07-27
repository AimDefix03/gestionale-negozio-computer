package it.giovannidefilippo.gestionale.company;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class CompanySettingsRequests {
    private CompanySettingsRequests() {
    }

    public record UpdateRequest(
            @NotNull @PositiveOrZero Long version,
            @Size(max = 160) String legalName,
            @Size(max = 32) String taxCode,
            @Size(max = 32) String vatNumber,
            @Size(max = 160) String email,
            @Size(max = 40) String phone,
            @Size(max = 300) String address,
            @Size(max = 16) String postalCode,
            @Size(max = 120) String city,
            @Size(max = 8) String province,
            @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "Il paese deve essere un codice ISO di due lettere.") String countryCode,
            @NotNull @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal defaultVatRate,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{1,8}$", message = "Il prefisso fattura deve contenere da 1 a 8 caratteri alfanumerici.") String invoicePrefix,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{1,8}$", message = "Il prefisso nota credito deve contenere da 1 a 8 caratteri alfanumerici.") String creditNotePrefix,
            @Min(3) @Max(8) int numberPadding
    ) {
    }
}
