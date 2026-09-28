package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.security.core.SpringSecurityCoreVersion;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeSecurityBaselineContractTest {
    @Test
    void runtimeUsesSupportedSpringLineAndSecurityVersionOutsideCve202622732Ranges() {
        String bootVersion = SpringBootVersion.getVersion();
        String securityVersion = SpringSecurityCoreVersion.getVersion();

        assertThat(major(bootVersion)).isGreaterThanOrEqualTo(4);
        assertThat(isAffectedByCve202622732(securityVersion)).isFalse();
    }

    @Test
    void distributableConfigurationIsFailClosedAndContainsNoKnownBootstrapSecret() throws Exception {
        String base = Files.readString(Path.of("src/main/resources/application.yml"));
        String development = Files.readString(Path.of("src/main/resources/application-dev.yml"));
        String production = Files.readString(Path.of("src/main/resources/application-prod.yml"));

        assertThat(base)
                .doesNotContain("profiles:\n    default:")
                .doesNotContain("Test-Bootstrap-9842!");
        assertThat(development)
                .contains("GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED:false")
                .doesNotContain("GESTIONALE_SEED_ADMIN_ENABLED")
                .doesNotContain("Test-Bootstrap-9842!");
        assertThat(production).doesNotContain("Test-Bootstrap-9842!");
        assertThat(Files.readString(Path.of(
                "src/main/java/it/giovannidefilippo/gestionale/common/SeedAdminInitializer.java"
        ))).doesNotContain("RootSecure123!");
    }

    @Test
    void h2ConsoleIsConfinedToTestsAndHasNoProductionSecurityBypass() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        String securityConfig = Files.readString(Path.of("src/main/java/it/giovannidefilippo/gestionale/security/SecurityConfig.java"));
        String sessionFilter = Files.readString(Path.of("src/main/java/it/giovannidefilippo/gestionale/security/SessionAuthenticationFilter.java"));

        assertThat(pom)
                .contains("<artifactId>h2</artifactId>")
                .containsPattern("(?s)<artifactId>h2</artifactId>\\s*<scope>test</scope>");
        assertThat(securityConfig).doesNotContain("/h2-console");
        assertThat(sessionFilter).doesNotContain("/h2-console");
    }

    @Test
    void securityWorkflowRunsABlockingBackendScaGate() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        String workflow = Files.readString(Path.of("../../.github/workflows/security.yml"));

        assertThat(pom)
                .contains("org.owasp")
                .contains("dependency-check-maven")
                .contains("<failBuildOnCVSS>7.0</failBuildOnCVSS>")
                .contains("<failOnError>true</failOnError>");
        assertThat(workflow)
                .contains("dependency-check:check")
                .contains("dependency-check-report.html")
                .contains("dependency-check-report.json");
    }

    private static int major(String version) {
        return Integer.parseInt(version.split("\\.")[0]);
    }

    private static boolean isAffectedByCve202622732(String version) {
        int[] parts = versionParts(version);
        if (parts[0] == 6 && parts[1] == 4) {
            return parts[2] <= 14;
        }
        if (parts[0] == 6 && parts[1] == 5) {
            return parts[2] <= 8;
        }
        return parts[0] == 7 && parts[1] == 0 && parts[2] <= 3;
    }

    private static int[] versionParts(String version) {
        String[] tokens = version.split("[.-]");
        return new int[]{Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])};
    }
}
