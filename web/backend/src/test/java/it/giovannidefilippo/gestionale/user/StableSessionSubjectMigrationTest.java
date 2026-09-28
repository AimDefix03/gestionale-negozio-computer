package it.giovannidefilippo.gestionale.user;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StableSessionSubjectMigrationTest {
    @Test
    void migratesSessionsToAccountIdInvalidatesLegacyTokensAndCascadesOnDelete() {
        String database = "stable_subject_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("19").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        jdbc.update(
                "insert into user_accounts (id, username, password_salt, password_hash, role) values (901, 'stable.user', 'salt', 'hash', 'CUSTOMER')"
        );
        jdbc.update(
                """
                insert into auth_sessions (
                    id, token_hash, username, role, created_at, expires_at, last_used_at, revoked_at
                ) values (
                    902, 'stable-token', 'stable.user', 'CUSTOMER',
                    current_timestamp, dateadd('HOUR', 1, current_timestamp), current_timestamp, null
                )
                """
        );
        jdbc.update(
                """
                insert into auth_sessions (
                    id, token_hash, username, role, created_at, expires_at, last_used_at, revoked_at
                ) values (
                    903, 'orphan-token', 'deleted.user', 'CUSTOMER',
                    current_timestamp, dateadd('HOUR', 1, current_timestamp), current_timestamp, null
                )
                """
        );

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("20").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("20");
        assertThat(jdbc.queryForObject(
                "select credential_version from user_accounts where id = 901",
                Long.class
        )).isEqualTo(1L);
        assertThat(jdbc.queryForObject(
                "select account_id from auth_sessions where id = 902",
                Long.class
        )).isEqualTo(901L);
        assertThat(jdbc.queryForObject(
                "select username_snapshot from auth_sessions where id = 902",
                String.class
        )).isEqualTo("stable.user");
        assertThat(jdbc.queryForObject(
                "select credential_version from auth_sessions where id = 902",
                Long.class
        )).isEqualTo(1L);
        assertThat(jdbc.queryForObject(
                "select revoked_at is not null from auth_sessions where id = 902",
                Boolean.class
        )).isTrue();
        assertThat(jdbc.queryForObject(
                "select count(*) from auth_sessions where token_hash = 'orphan-token'",
                Long.class
        )).isZero();

        jdbc.update("delete from user_accounts where id = 901");

        assertThat(jdbc.queryForObject(
                "select count(*) from auth_sessions where id = 902",
                Long.class
        )).isZero();
    }
}
