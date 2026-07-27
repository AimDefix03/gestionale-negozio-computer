package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringProdLikeWorkflowTest {
    @Test
    void workflowSchedulesTheSingleProdLikeRunnerAndAlwaysPublishesDiagnostics() throws Exception {
        String workflow = Files.readString(Path.of("../../.github/workflows/ci.yml"));

        assertThat(workflow)
                .contains("schedule:")
                .contains("cron: \"23 3 * * 1\"")
                .contains("timeout-minutes: 60")
                .contains("sh scripts/ci/run-prod-like-verification.sh")
                .contains("name: prod-like-diagnostics-${{ github.sha }}-${{ github.run_id }}-${{ github.run_attempt }}")
                .contains("if: always()")
                .contains("if-no-files-found: error")
                .contains("test -f docs/PROD_LIKE_VERIFICATION.md");
    }

    @Test
    void runnerCoversImagesMigrationsSecurityObservabilityBrowserAndRecovery() throws Exception {
        String runner = Files.readString(Path.of("../../scripts/ci/run-prod-like-verification.sh"));

        assertThat(runner)
                .contains("compose build --pull")
                .contains("verify-flyway-migrations.sh")
                .contains("verify-actuator-exposure.sh")
                .contains("verify-container-secrets.sh")
                .contains("verify-container-hardening.sh")
                .contains("verify-browser-security.sh")
                .contains("verify-runtime-observability.sh")
                .contains("verify-prometheus-hardening.sh")
                .contains("/api/v1/targets")
                .contains("npm run test:e2e")
                .contains("verify-login-rate-limit.sh")
                .contains("test-backup-lifecycle.sh")
                .contains("verify-backup-schedule.sh")
                .contains("verify-backup-restore.sh")
                .contains("GESTIONALE_MANAGEMENT_PORT=9090")
                .contains("PRODLIKE_BACKUP_VERIFY_POSTGRES_PORT:-55433")
                .contains("PRODLIKE_RESTORE_DRILL_POSTGRES_PORT:-55435")
                .contains("Le porte prod-like devono essere distinte");

        String backupRestore = Files.readString(Path.of("../../scripts/db/verify-backup-restore.sh"));
        assertThat(backupRestore)
                .contains("BACKUP_VERIFY_POSTGRES_PORT:-55433")
                .contains("RESTORE_DRILL_POSTGRES_PORT=");
    }

    @Test
    void runnerCapturesDiagnosticsBeforeRemovingAndVerifyingDockerResources() throws Exception {
        String runner = Files.readString(Path.of("../../scripts/ci/run-prod-like-verification.sh"));
        String cleanupVerifier = Files.readString(Path.of("../../scripts/ci/verify-prod-like-cleanup.sh"));
        int cleanupStart = runner.indexOf("cleanup() {");
        int capture = runner.indexOf("  capture_diagnostics\n", cleanupStart);
        int composeDown = runner.indexOf("  compose down -v --remove-orphans", cleanupStart);

        assertThat(cleanupStart).isGreaterThanOrEqualTo(0);
        assertThat(capture).isGreaterThan(cleanupStart);
        assertThat(composeDown).isGreaterThan(capture);
        assertThat(runner)
                .contains("trap cleanup EXIT INT TERM")
                .contains("verify-prod-like-cleanup.sh");
        assertThat(cleanupVerifier)
                .contains("com.docker.compose.project=$PROJECT_NAME")
                .contains("docker ps -aq")
                .contains("docker volume ls -q")
                .contains("docker network ls -q");
    }
}
