package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MvpEndToEndGateContractTest {
    @Test
    void browserGateCoversEveryMandatoryVerticalScenario() throws Exception {
        String suite = readRoot("web/frontend/e2e/mvp-gate.spec.ts");

        assertThat(suite)
                .contains("super admin crea dipendente")
                .contains("dipendente crea cliente e vendita")
                .contains("carico rettifica riserva ed evasione")
                .contains("acconto, annullo e storno")
                .contains("reso multi-riga, ricezione e rimborso")
                .contains("sessione scaduta durante query")
                .contains("autorizzazione cliente negativa")
                .contains("inventory\\/initial-balance")
                .contains("inventory\\/adjustments")
                .contains("/payments/receipts")
                .contains("/returns/")
                .contains("Puoi operare solo sui tuoi ordini.");
    }

    @Test
    void localMvpGateCombinesProdLikeBrowserAndPopulatedDatabaseUpgrade() throws Exception {
        String runner = readRoot("scripts/e2e/run-mvp-gate.sh");
        String databaseRunner = readRoot("scripts/db/verify-postgresql-suite.sh");

        assertThat(runner)
                .contains("run-prod-like-verification.sh")
                .contains("verify-postgresql-suite.sh\" fast");
        assertThat(databaseRunner)
                .contains("mvn -B -Ppostgresql-it test")
                .contains("verify-historical-fixtures.sh\" small upgrades");
    }

    @Test
    void ciRequiresPostgreSqlAndProdLikeWhileCrossBrowserAccessibilityRemainsEnabled() throws Exception {
        String workflow = readRoot(".github/workflows/ci.yml");
        String playwright = readRoot("web/frontend/playwright.config.ts");

        assertThat(workflow)
                .contains("name: Database migrations")
                .contains("verify-postgresql-suite.sh fast")
                .contains("name: Prod-like Docker stack")
                .contains("run-prod-like-verification.sh")
                .contains("- database-migrations")
                .contains("- prod-like-stack");
        assertThat(playwright)
                .contains("name: 'firefox'")
                .contains("name: 'webkit'")
                .contains("testMatch: '**/accessibility.spec.ts'");
    }

    private static String readRoot(String path) throws Exception {
        return Files.readString(Path.of("../..", path));
    }
}
