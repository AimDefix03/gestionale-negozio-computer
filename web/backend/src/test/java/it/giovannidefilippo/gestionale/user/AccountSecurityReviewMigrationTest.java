package it.giovannidefilippo.gestionale.user;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountSecurityReviewMigrationTest {
    @Test
    void classifiesHistoricalAccountsPreservesEvidenceAndRevokesSuspiciousSessions() {
        String database = "account_review_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("18").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        insertAccount(jdbc, 1, "customer_self", "CUSTOMER");
        insertAccount(jdbc, 2, "employee_self", "EMPLOYEE");
        insertAccount(jdbc, 3, "employee_admin", "EMPLOYEE");
        insertAccount(jdbc, 4, "admin_unknown", "ADMIN");
        insertCreationAudit(jdbc, "customer_self", "Sistema", "Registrazione pubblica - ruolo Cliente");
        insertCreationAudit(jdbc, "employee_self", "Sistema", "Registrazione pubblica - ruolo Dipendente");
        insertCreationAudit(jdbc, "employee_admin", "root", "Creazione da pannello admin - ruolo Dipendente");
        insertSession(jdbc, 1, "customer_self", "CUSTOMER");
        insertSession(jdbc, 2, "employee_self", "EMPLOYEE");
        insertSession(jdbc, 3, "employee_admin", "EMPLOYEE");

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("19").load();
        upgrade.migrate();

        assertThat(value(jdbc, "customer_self", "provisioning_source")).isEqualTo("SELF_SERVICE");
        assertThat(verified(jdbc, "customer_self")).isTrue();
        assertThat(value(jdbc, "employee_self", "provisioning_source")).isEqualTo("SELF_SERVICE");
        assertThat(verified(jdbc, "employee_self")).isFalse();
        assertThat(value(jdbc, "employee_admin", "provisioning_source")).isEqualTo("ADMIN_PROVISIONED");
        assertThat(verified(jdbc, "employee_admin")).isTrue();
        assertThat(verified(jdbc, "admin_unknown")).isTrue();
        assertThat(sessionRevoked(jdbc, "employee_self")).isTrue();
        assertThat(sessionRevoked(jdbc, "employee_admin")).isFalse();
        assertThat(sessionRevoked(jdbc, "customer_self")).isFalse();
        assertThat(jdbc.queryForObject(
                "select count(*) from audit_events where action = 'QUARANTINE_OPERATIONAL_ACCOUNT' and target = 'employee_self'",
                Long.class
        )).isEqualTo(1L);
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("19");
    }

    private void insertAccount(JdbcTemplate jdbc, long id, String username, String role) {
        jdbc.update(
                "insert into user_accounts (id, username, password_salt, password_hash, role) values (?, ?, 'salt', 'hash', ?)",
                id,
                username,
                role
        );
    }

    private void insertCreationAudit(JdbcTemplate jdbc, String target, String actor, String details) {
        jdbc.update(
                """
                insert into audit_events (
                    timestamp, actor, role, action, target, details, category, severity, request_id, source, entity_type
                ) values (
                    current_timestamp, ?, 'Sistema', 'CREATE_ACCOUNT', ?, ?, 'ACCOUNT', 'WARNING', '-', 'SYSTEM', 'USER_ACCOUNT'
                )
                """,
                actor,
                target,
                details
        );
    }

    private void insertSession(JdbcTemplate jdbc, long id, String username, String role) {
        jdbc.update(
                """
                insert into auth_sessions (
                    id, token_hash, username, role, created_at, expires_at, last_used_at, revoked_at
                ) values (
                    ?, ?, ?, ?, current_timestamp, dateadd('HOUR', 1, current_timestamp), current_timestamp, null
                )
                """,
                id,
                "token-" + id,
                username,
                role
        );
    }

    private String value(JdbcTemplate jdbc, String username, String column) {
        return jdbc.queryForObject(
                "select " + column + " from user_accounts where username = ?",
                String.class,
                username
        );
    }

    private boolean verified(JdbcTemplate jdbc, String username) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select operational_access_verified from user_accounts where username = ?",
                Boolean.class,
                username
        ));
    }

    private boolean sessionRevoked(JdbcTemplate jdbc, String username) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select revoked_at is not null from auth_sessions where username = ?",
                Boolean.class,
                username
        ));
    }
}
