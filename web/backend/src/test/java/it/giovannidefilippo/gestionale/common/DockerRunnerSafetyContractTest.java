package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DockerRunnerSafetyContractTest {
    private static final Path ROOT = Path.of("../..");

    @Test
    void runnersUseRandomPrefixedProjectsAndRefuseExistingProjectsBeforeStarting() throws Exception {
        String prodLike = read("scripts/ci/run-prod-like-verification.sh");
        String e2e = read("scripts/e2e/run-web-smoke.sh");

        assertSafeProjectSetup(prodLike, "gestionale-prodlike-");
        assertSafeProjectSetup(e2e, "gestionale-e2e-");
    }

    @Test
    void runnersNeverUseComposeDownAndDelegateCleanupToTheLabelGuard() throws Exception {
        String prodLike = read("scripts/ci/run-prod-like-verification.sh");
        String e2e = read("scripts/e2e/run-web-smoke.sh");

        for (String runner : new String[]{prodLike, e2e}) {
            assertThat(runner)
                    .doesNotContain("compose down")
                    .contains("docker_run_cleanup")
                    .contains("docker_run_inventory")
                    .contains("trap cleanup EXIT")
                    .contains("trap 'handle_signal 1' HUP")
                    .contains("trap 'handle_signal 2' INT")
                    .contains("trap 'handle_signal 15' TERM");
        }
    }

    @Test
    void labelOverrideCoversEveryRuntimeResource() throws Exception {
        String labels = read("docker-compose.run-labels.yml");

        assertThat(labels)
                .contains("postgres:")
                .contains("backend:")
                .contains("frontend:")
                .contains("prometheus:")
                .contains("gestionale-prodlike-postgres-data:")
                .contains("gestionale-prodlike-prometheus-data:")
                .contains("default:");
        assertThat(count(labels, "it.giovannidefilippo.gestionale.run-id"))
                .isGreaterThanOrEqualTo(7);
    }

    @Test
    void cleanupGuardInventoriesAndDeletesOnlyResourcesWithBothLabels() throws Exception {
        String guard = read("scripts/ci/docker-run-safety.sh");
        String recovery = read("scripts/ci/cleanup-docker-run.sh");

        assertThat(guard)
                .contains("com.docker.compose.project")
                .contains("it.giovannidefilippo.gestionale.run-id")
                .contains("docker_run_assert_project_unused")
                .contains("docker_run_assert_project_owned")
                .contains("docker_run_inventory")
                .contains("docker rm -f")
                .contains("docker volume rm")
                .contains("docker network rm");
        assertThat(recovery)
                .contains("docker_run_validate_project_name")
                .contains("docker_run_cleanup")
                .contains("gestionale-prodlike-")
                .contains("gestionale-e2e-");
    }

    @Test
    void runtimeSafetyTestCoversSentinelCollisionSignalsAndCrashRecovery() throws Exception {
        String runtimeTest = read("scripts/ci/test-docker-run-safety.sh");

        assertThat(runtimeTest)
                .contains("sentinel-proof")
                .contains("collision")
                .contains("HUP")
                .contains("INT")
                .contains("TERM")
                .contains("KILL")
                .contains("docker_run_cleanup");
    }

    @Test
    void continuousIntegrationRunsProdLikeAndRequiresItInTheQualityGate() throws Exception {
        String workflow = read(".github/workflows/ci.yml");
        int prodLikeJob = workflow.indexOf("  prod-like-stack:");
        int qualityGate = workflow.indexOf("  quality-gate:");

        assertThat(prodLikeJob).isGreaterThanOrEqualTo(0);
        assertThat(qualityGate).isGreaterThan(prodLikeJob);
        assertThat(workflow.substring(prodLikeJob, qualityGate))
                .doesNotContain("if: ${{ false }}")
                .contains("sh scripts/ci/run-prod-like-verification.sh");
        assertThat(workflow.substring(qualityGate))
                .contains("- prod-like-stack");
    }

    private static void assertSafeProjectSetup(String runner, String prefix) {
        int preflight = runner.indexOf("docker_run_assert_project_unused");
        int firstUp = runner.indexOf("compose up");

        assertThat(runner)
                .contains(prefix)
                .contains("openssl rand -hex")
                .contains("docker-compose.run-labels.yml")
                .contains("GESTIONALE_RUN_ID");
        assertThat(preflight).isGreaterThanOrEqualTo(0);
        assertThat(firstUp).isGreaterThan(preflight);
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private static int count(String value, String fragment) {
        return (value.length() - value.replace(fragment, "").length()) / fragment.length();
    }
}
