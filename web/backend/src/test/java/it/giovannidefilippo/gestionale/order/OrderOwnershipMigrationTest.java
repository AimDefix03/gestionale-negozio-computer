package it.giovannidefilippo.gestionale.order;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderOwnershipMigrationTest {
    @Test
    void linksOnlyExactPartnerCodesAndLeavesTextualOwnershipUnresolved() {
        String database = "order_ownership_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("20").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        insertAccount(jdbc, 701, "same-name", "CUSTOMER");
        insertPartner(jdbc, 801, "CLI-EXACT", "Nome duplicabile");
        insertPartner(jdbc, 802, "CLI-SECOND", "Nome duplicabile");
        insertOrder(jdbc, 901, "ORD-PARTNER", "Nome duplicabile", "CLI-EXACT");
        insertOrder(jdbc, 902, "ORD-TEXT", "same-name", "");
        insertOrder(jdbc, 903, "ORD-UNKNOWN", "Nome senza anagrafica", "CLI-MISSING");

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("23").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("23");
        assertThat(value(jdbc, "ORD-PARTNER", "partner_id", Long.class)).isEqualTo(801L);
        assertThat(value(jdbc, "ORD-PARTNER", "ownership_status", String.class)).isEqualTo("PARTNER");
        assertThat(value(jdbc, "ORD-TEXT", "customer_account_id", Long.class)).isNull();
        assertThat(value(jdbc, "ORD-TEXT", "ownership_status", String.class)).isEqualTo("UNRESOLVED");
        assertThat(value(jdbc, "ORD-UNKNOWN", "partner_id", Long.class)).isNull();
        assertThat(value(jdbc, "ORD-UNKNOWN", "ownership_status", String.class)).isEqualTo("UNRESOLVED");

        jdbc.update("update customer_orders set customer_account_id = 701, ownership_status = 'ACCOUNT' where code = 'ORD-TEXT'");
        jdbc.update("delete from user_accounts where id = 701");

        assertThat(value(jdbc, "ORD-TEXT", "customer_account_id", Long.class)).isNull();
    }

    private void insertAccount(JdbcTemplate jdbc, long id, String username, String role) {
        jdbc.update(
                "insert into user_accounts (id, username, password_salt, password_hash, role) values (?, ?, 'salt', 'hash', ?)",
                id,
                username,
                role
        );
    }

    private void insertPartner(JdbcTemplate jdbc, long id, String code, String displayName) {
        jdbc.update(
                """
                insert into business_partners (
                    id, code, type, display_name, active, created_at, updated_at
                ) values (?, ?, 'CUSTOMER', ?, true, current_timestamp, current_timestamp)
                """,
                id,
                code,
                displayName
        );
    }

    private void insertOrder(JdbcTemplate jdbc, long id, String code, String customer, String customerCode) {
        jdbc.update(
                """
                insert into customer_orders (
                    id, code, customer, customer_code, timestamp, payment_method, total, status, status_changed_at
                ) values (?, ?, ?, ?, current_timestamp, 'Carta', 10.00, 'DRAFT', current_timestamp)
                """,
                id,
                code,
                customer,
                customerCode
        );
    }

    private <T> T value(JdbcTemplate jdbc, String orderCode, String column, Class<T> type) {
        return jdbc.queryForObject("select " + column + " from customer_orders where code = ?", type, orderCode);
    }
}
