package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationTemplateTest {
    @Test
    void environmentExamplesUsePlaceholdersAndRealEnvFilesAreIgnored() throws Exception {
        String backendExample = Files.readString(Path.of(".env.example"));
        String frontendExample = Files.readString(Path.of("../frontend/.env.example"));
        String dockerExample = Files.readString(Path.of("../../.env.docker.example"));
        String dockerSecrets = Files.readString(Path.of("../../docker-compose.secrets.yml"));
        String bootstrapSecret = Files.readString(Path.of("../../docker-compose.bootstrap-secret.yml"));
        String prodLikeCompose = Files.readString(Path.of("../../docker-compose.prod-like.yml"));
        String backendDockerfile = Files.readString(Path.of("Dockerfile"));
        String frontendDockerfile = Files.readString(Path.of("../frontend/Dockerfile"));
        String postgresDockerfile = Files.readString(Path.of("../postgres/Dockerfile"));
        String gitignore = Files.readString(Path.of("../../.gitignore"));

        assertThat(backendExample)
                .contains("GESTIONALE_DB_PASSWORD_FILE=/run/secrets/gestionale_db_password")
                .contains("GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_FILE=/run/secrets/gestionale_bootstrap_password")
                .contains("GESTIONALE_SECURITY_SESSION_DURATION_MINUTES=45")
                .contains("GESTIONALE_SECURITY_SESSION_IDLE_TIMEOUT_MINUTES=30")
                .contains("GESTIONALE_SECURITY_SESSION_TOUCH_INTERVAL_SECONDS=60")
                .contains("GESTIONALE_MANAGEMENT_PORT=9090")
                .doesNotContain("Admin123!")
                .doesNotContain("gestionale_dev_password");

        assertThat(frontendExample).doesNotContain("=");

        assertThat(dockerExample)
                .contains("GESTIONALE_DB_PASSWORD_SECRET_FILE=./secrets/database-password")
                .contains("GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE=./secrets/bootstrap-super-admin-password")
                .contains("GESTIONALE_SECURITY_SESSION_DURATION_MINUTES=45")
                .contains("GESTIONALE_SECURITY_SESSION_IDLE_TIMEOUT_MINUTES=30")
                .contains("GESTIONALE_SECURITY_SESSION_TOUCH_INTERVAL_SECONDS=60")
                .contains("GESTIONALE_MANAGEMENT_PORT=9090")
                .contains("GESTIONALE_PROMETHEUS_PORT=9091")
                .contains("GESTIONALE_LOG_MAX_SIZE=10m")
                .contains("GESTIONALE_LOG_MAX_FILES=5")
                .contains("GESTIONALE_METRICS_RETENTION=15d")
                .contains("BACKUP_RETENTION_DAYS=14")
                .contains("BACKUP_RETENTION_COUNT=30")
                .contains("BACKUP_MAX_AGE_HOURS=26")
                .contains("RESTORE_DRILL_MAX_SECONDS=900")
                .doesNotContain("Admin123!")
                .doesNotContain("gestionale_dev_password");

        assertThat(dockerSecrets)
                .contains("/run/secrets/gestionale_db_password")
                .contains("GESTIONALE_DB_PASSWORD_SECRET_FILE")
                .doesNotContain("replace-with-prodlike-database-password");

        assertThat(bootstrapSecret)
                .contains("/run/secrets/gestionale_bootstrap_password")
                .contains("GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE")
                .doesNotContain("replace-with-strong-initial-super-admin-password");

        assertThat(prodLikeCompose.split("read_only: true", -1)).hasSize(5);
        assertThat(prodLikeCompose.split("no-new-privileges:true", -1)).hasSize(5);
        assertThat(prodLikeCompose.split("- ALL", -1)).hasSize(5);
        assertThat(prodLikeCompose)
                .contains("/var/run/postgresql:rw,noexec,nosuid,nodev")
                .contains("${GESTIONALE_FRONTEND_PORT:-8081}:8080")
                .contains("prom/prometheus:v3.13.1")
                .contains("127.0.0.1:${GESTIONALE_PROMETHEUS_PORT:-9091}:9090");

        assertThat(backendDockerfile).contains("USER 10001:10001");
        assertThat(frontendDockerfile)
                .contains("USER nginx")
                .contains("EXPOSE 8080");
        assertThat(postgresDockerfile).contains("USER postgres");

        assertThat(gitignore)
                .contains(".env")
                .contains("!.env.example")
                .contains("!.env.docker.example")
                .contains("web/backend/.env")
                .contains("web/frontend/.env")
                .contains("*.dump.sha256")
                .contains("*.dump.tmp")
                .contains("secrets/");
    }
}
