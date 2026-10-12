package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseLeastPrivilegeContractTest {
    private static String read(String path) throws Exception {
        return Files.readString(Path.of("../..", path));
    }

    @Test
    void productionSeparatesRuntimeMigrationsBackupAndRestore() throws Exception {
        String compose = read("docker-compose.prod-like.yml");
        String secrets = read("docker-compose.secrets.yml");
        String production = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        String backendDockerfile = Files.readString(Path.of("Dockerfile"));
        String postgresDockerfile = Files.readString(Path.of("../postgres/Dockerfile"));
        String roleSql = read("scripts/db/postgresql-roles.sql");
        String grantsSql = read("scripts/db/postgresql-database-grants.sql");
        String backup = read("scripts/db/backup.sh");
        String restore = read("scripts/db/restore.sh");
        String prodLikeRunner = read("scripts/ci/run-prod-like-verification.sh");

        assertThat(compose)
                .contains("127.0.0.1:${GESTIONALE_POSTGRES_PORT:-5433}:5432")
                .contains("GESTIONALE_DB_RUNTIME_USERNAME")
                .contains("GESTIONALE_DB_MIGRATOR_USERNAME")
                .contains("GESTIONALE_DB_OWNER_USERNAME")
                .contains("GESTIONALE_DB_BACKUP_USERNAME")
                .contains("GESTIONALE_DB_RESTORE_USERNAME")
                .doesNotContain("GESTIONALE_DB_USERNAME:");

        assertThat(production)
                .contains("username: ${GESTIONALE_DB_RUNTIME_USERNAME}")
                .contains("user: ${GESTIONALE_DB_MIGRATOR_USERNAME}")
                .contains("init-sqls: SET ROLE ${GESTIONALE_DB_OWNER_USERNAME}");

        assertThat(backendDockerfile)
                .contains("GESTIONALE_DB_RUNTIME_PASSWORD")
                .contains("GESTIONALE_DB_MIGRATOR_PASSWORD");
        assertThat(postgresDockerfile)
                .contains("10-create-application-roles.sh")
                .contains("postgresql-roles.sql");

        assertThat(roleSql)
                .contains("NOLOGIN NOINHERIT NOSUPERUSER")
                .contains("NOBYPASSRLS")
                .contains("GRANT %I TO %I");
        assertThat(grantsSql)
                .contains("REVOKE CREATE ON SCHEMA public FROM PUBLIC")
                .contains("GRANT SELECT, INSERT, UPDATE, DELETE")
                .contains("GRANT SELECT ON ALL TABLES")
                .doesNotContain("GRANT ALL");

        assertThat(backup)
                .contains("GESTIONALE_DB_BACKUP_USERNAME")
                .doesNotContain("GESTIONALE_DB_RUNTIME_USERNAME");
        assertThat(restore)
                .contains("GESTIONALE_DB_RESTORE_USERNAME")
                .contains("--role=\"$GESTIONALE_DB_OWNER_USERNAME\"")
                .doesNotContain("GESTIONALE_DB_RUNTIME_USERNAME\" -d");

        assertThat(secrets)
                .contains("gestionale_db_bootstrap_password")
                .contains("gestionale_db_migrator_password")
                .contains("gestionale_db_runtime_password")
                .contains("gestionale_db_backup_password")
                .contains("gestionale_db_restore_password");
        assertThat(prodLikeRunner)
                .contains("verify-backup-restore.sh")
                .contains("verify-database-least-privilege.sh");
    }
}
