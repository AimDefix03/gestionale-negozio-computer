package it.giovannidefilippo.gestionale.company;

import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CompanySettingsServiceTest {
    @Autowired
    private CompanySettingsService service;

    @Test
    void updatesAndNormalizesCompanySettings() {
        CompanySettingsResponse current = service.current();

        CompanySettingsResponse updated = service.update(request(
                current.version(), "  Azienda Test  ", " cf-test ", " it001 ", new BigDecimal("0.1000"),
                current.invoicePrefix().toLowerCase(), current.creditNotePrefix().toLowerCase(), current.numberPadding()
        ), actor());

        assertThat(updated.version()).isGreaterThan(current.version());
        assertThat(updated.configured()).isTrue();
        assertThat(updated.legalName()).isEqualTo("Azienda Test");
        assertThat(updated.taxCode()).isEqualTo("cf-test");
        assertThat(updated.vatNumber()).isEqualTo("it001");
        assertThat(updated.defaultVatRate()).isEqualByComparingTo("0.1");
        assertThat(updated.invoicePrefix()).isEqualTo(current.invoicePrefix());
        assertThat(updated.creditNotePrefix()).isEqualTo(current.creditNotePrefix());
        assertThat(updated.updatedBy()).isEqualTo("admin");
    }

    @Test
    void rejectsStaleVersion() {
        CompanySettingsResponse current = service.current();
        service.update(request(
                current.version(), "Azienda Test", "CF-TEST", "", current.defaultVatRate(),
                current.invoicePrefix(), current.creditNotePrefix(), current.numberPadding()
        ), actor());

        assertThatThrownBy(() -> service.update(request(
                current.version(), "Modifica obsoleta", "CF-TEST", "", current.defaultVatRate(),
                current.invoicePrefix(), current.creditNotePrefix(), current.numberPadding()
        ), actor()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("altra sessione");
    }

    @Test
    void rejectsEqualDocumentPrefixes() {
        CompanySettingsResponse current = service.current();

        assertThatThrownBy(() -> service.update(request(
                current.version(), "Azienda Test", "CF-TEST", "", current.defaultVatRate(),
                "DOC", "doc", current.numberPadding()
        ), actor()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("devono essere diversi");
    }

    private static CompanySettingsRequests.UpdateRequest request(
            long version,
            String legalName,
            String taxCode,
            String vatNumber,
            BigDecimal vatRate,
            String invoicePrefix,
            String creditNotePrefix,
            int padding
    ) {
        return new CompanySettingsRequests.UpdateRequest(
                version, legalName, taxCode, vatNumber, "amministrazione@example.com", "+39 0000000000",
                "Via Test 1", "80100", "Napoli", "NA", "it", vatRate,
                invoicePrefix, creditNotePrefix, padding
        );
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }
}
