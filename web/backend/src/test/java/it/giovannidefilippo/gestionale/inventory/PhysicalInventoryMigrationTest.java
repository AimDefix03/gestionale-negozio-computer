package it.giovannidefilippo.gestionale.inventory;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhysicalInventoryMigrationTest {
    @Test
    void addsGovernedPhysicalInventoryWithoutChangingHistoricalStock() {
        String database = "physical_inventory_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("34").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("insert into products (id, version, code, code_canonical, name, description, category, brand, product_type, usage_context, quantity, reserved_quantity, price, discount, discontinued, costed_quantity) values (5201, 0, 'INV-PRD', 'inv-prd', 'Prodotto inventario', 'Fixture inventario fisico.', 'HARDWARE', 'Brand', 'Componente', '', 10, 2, 100, 0, false, 0)");

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("35").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("35");
        assertThat(jdbc.queryForObject("select quantity from products where id = 5201", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from physical_inventory_sessions", Long.class)).isZero();

        insertOpenSession(jdbc, 5301, "INV-TEST-1");
        insertOpenItem(jdbc, 5401, 5301);
        insertOpenSession(jdbc, 5302, "INV-TEST-2");

        assertThatThrownBy(() -> insertOpenItem(jdbc, 5402, 5302))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update physical_inventory_sessions set status = 'APPROVED', submitted_at = current_timestamp, submitted_by = 'operator', submitted_by_role = 'Dipendente', approved_at = current_timestamp, approved_by = 'operator', approved_by_role = 'Dipendente', approval_reason = 'Auto approvazione' where id = 5301"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertOpenSession(JdbcTemplate jdbc, long id, String code) {
        jdbc.update("insert into physical_inventory_sessions (id, version, code, status, reason, created_at, created_by, created_by_role) values (?, 0, ?, 'OPEN', 'Conteggio periodico', current_timestamp, 'operator', 'Dipendente')", id, code);
    }

    private void insertOpenItem(JdbcTemplate jdbc, long id, long sessionId) {
        jdbc.update("insert into physical_inventory_items (id, session_id, product_id, product_code_snapshot, product_name_snapshot, theoretical_quantity_snapshot, reserved_quantity_snapshot, product_version_snapshot, active_marker) values (?, ?, 5201, 'INV-PRD', 'Prodotto inventario', 10, 2, 0, true)", id, sessionId);
    }
}
