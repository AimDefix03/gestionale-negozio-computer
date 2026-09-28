package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.company.CompanySettingsRequests;
import it.giovannidefilippo.gestionale.company.CompanySettingsResponse;
import it.giovannidefilippo.gestionale.company.CompanySettingsService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;

public final class TestCompanySettings {
    private TestCompanySettings() {
    }

    public static CompanySettingsResponse configure(CompanySettingsService service, AuthenticatedUser actor) {
        CompanySettingsResponse current = service.current();
        if (current.configured()) {
            return current;
        }
        return service.update(new CompanySettingsRequests.UpdateRequest(
                current.version(),
                "Impresa Test Srl",
                "TSTNGZ80A01F839A",
                "01234567890",
                "amministrazione@example.invalid",
                "+39 0810000000",
                "Via Test 1",
                "80100",
                "Napoli",
                "NA",
                "IT",
                "Europe/Rome",
                current.defaultVatRate(),
                current.invoicePrefix(),
                current.creditNotePrefix(),
                current.numberPadding()
        ), actor);
    }
}
