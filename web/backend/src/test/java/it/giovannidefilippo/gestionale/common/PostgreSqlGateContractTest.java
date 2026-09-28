package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlGateContractTest {
    private static final List<String> POSTGRESQL_CRITICAL_TESTS = List.of(
            "user/AuthSessionServiceTest.java",
            "user/StableSessionSubjectTest.java",
            "common/CanonicalBusinessIdentityIntegrationTest.java",
            "product/ProductStockConcurrencyTest.java",
            "inventory/InventoryServiceTest.java",
            "inventory/InventoryLedgerControllerTest.java",
            "idempotency/IdempotencyControllerTest.java",
            "order/OrderCancellationIntegrationTest.java",
            "order/FinancialReconciliationIntegrationTest.java",
            "document/FiscalDocumentCodeSequenceTest.java",
            "order/OrderCodeSequenceTest.java",
            "order/OrderPaymentIntegrationTest.java",
            "security/AuthorizationSecurityTest.java",
            "security/ModuleAuthorizationMatrixTest.java"
    );

    @Test
    void databaseCriticalRegressionsRunAgainstRealPostgreSql() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        String runner = readRoot("scripts/db/verify-postgresql-suite.sh");
        String workflow = readRoot(".github/workflows/ci.yml");

        assertThat(pom)
                .contains("<id>postgresql-it</id>")
                .contains("testcontainers-postgresql")
                .contains("<groups>postgresql</groups>");
        assertThat(runner)
                .contains("mvn -B -Ppostgresql-it test")
                .contains("verify-historical-fixtures.sh\" small upgrades")
                .contains("verify-historical-fixtures.sh\" large upgrade-v18");
        assertThat(workflow)
                .contains("name: Database migrations")
                .contains("verify-postgresql-suite.sh fast")
                .contains("verify-postgresql-suite.sh nightly");

        for (String test : POSTGRESQL_CRITICAL_TESTS) {
            String source = Files.readString(Path.of("src/test/java/it/giovannidefilippo/gestionale", test));
            assertThat(source)
                    .as(test)
                    .contains("@Tag(\"postgresql\")")
                    .contains("extends PostgreSqlIntegrationTestSupport");
        }
    }

    @Test
    void productionSchemaRemainsMigrationOwned() throws Exception {
        String production = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        String fixtureRunner = readRoot("scripts/db/verify-historical-fixtures.sh");

        assertThat(production)
                .contains("ddl-auto: validate")
                .doesNotContain("ddl-auto: update")
                .doesNotContain("ddl-auto: create");
        assertThat(fixtureRunner)
                .contains("CURRENT_VERSION=35")
                .contains("pre-v23-canonical-resolution.sql")
                .contains("assert_snapshot \"$database\" \"$CURRENT_VERSION\"");
    }

    private static String readRoot(String path) throws Exception {
        return Files.readString(Path.of("../..", path));
    }
}
