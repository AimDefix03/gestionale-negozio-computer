package it.giovannidefilippo.gestionale.idempotency;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DurableIdempotencyMigrationTest {
    @Test
    void upgradesLegacyRecordsToStableAccountClaims() {
        String database = "durable_idempotency_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("24").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        jdbc.update("insert into user_accounts (id, username, username_canonical, password_salt, password_hash, role) values (501, 'stable.actor', 'stable.actor', 'salt', 'hash', 'EMPLOYEE')");
        jdbc.update(
                "insert into idempotency_records (id, idem_key, actor, operation, request_hash, status, response_body, response_type, http_status, created_at, completed_at) values (601, 'legacy-key', 'stable.actor', 'POST /legacy', ?, 'COMPLETED', '{\"value\":\"ok\"}', 'java.lang.String', 200, current_timestamp, current_timestamp)",
                "a".repeat(64)
        );

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("25").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("25");
        assertThat(jdbc.queryForObject("select actor_account_id from idempotency_records where id = 601", Long.class)).isEqualTo(501L);
        assertThat(jdbc.queryForObject("select fingerprint_version from idempotency_records where id = 601", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from idempotency_records where id = 601", String.class)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select expires_at is not null from idempotency_records where id = 601", Boolean.class)).isTrue();
    }
}
