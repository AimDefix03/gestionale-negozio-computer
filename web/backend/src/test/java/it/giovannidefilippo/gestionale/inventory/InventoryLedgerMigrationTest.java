package it.giovannidefilippo.gestionale.inventory;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryLedgerMigrationTest {
    @Test
    void establishesAuthoritativeBaselinesAndClassifiesHistoricalMovements() {
        TestDatabase database = database();
        database.migrateTo23();
        database.insertProduct(701, "LEG-001", 7);
        database.insertProduct(702, "ZERO-001", 0);
        database.insertLegacyMovement("LEG-001", 2, 5, 7);
        database.insertLegacyMovement("ORPHAN-001", 1, 0, 1);

        Flyway upgrade = database.upgrade();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("24");
        assertThat(database.integer("select count(*) from stock_movements where type = 'INITIAL_BALANCE' and authoritative = true"))
                .isEqualTo(2);
        assertThat(database.integer("select delta_quantity from stock_movements where product_id = 701 and type = 'INITIAL_BALANCE'"))
                .isEqualTo(7);
        assertThat(database.integer("select delta_quantity from stock_movements where product_id = 702 and type = 'INITIAL_BALANCE'"))
                .isZero();
        assertThat(database.text("select origin from stock_movements where product_code = 'LEG-001' and type = 'LOAD'"))
                .isEqualTo("LEGACY");
        assertThat(database.bool("select authoritative from stock_movements where product_code = 'LEG-001' and type = 'LOAD'"))
                .isFalse();
        assertThat(database.longValue("select product_id from stock_movements where product_code = 'LEG-001' and type = 'LOAD'"))
                .isEqualTo(701L);
        assertThat(database.longValue("select product_id from stock_movements where product_code = 'ORPHAN-001'"))
                .isNull();
        assertThat(database.integer("select coalesce(sum(delta_quantity), 0) from stock_movements where product_id = 701 and authoritative = true"))
                .isEqualTo(7);
    }

    private TestDatabase database() {
        String name = "inventory_ledger_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        return new TestDatabase(dataSource, new JdbcTemplate(dataSource));
    }

    private record TestDatabase(DriverManagerDataSource dataSource, JdbcTemplate jdbc) {
        void migrateTo23() {
            Flyway.configure().dataSource(dataSource).target("23").load().migrate();
        }

        Flyway upgrade() {
            Flyway flyway = Flyway.configure().dataSource(dataSource).target("24").load();
            flyway.migrate();
            return flyway;
        }

        void insertProduct(long id, String code, int quantity) {
            jdbc.update(
                    "insert into products (id, version, code, code_canonical, name, description, category, brand, product_type, usage_context, quantity, reserved_quantity, price, discount, discontinued) values (?, 0, ?, ?, 'Fixture', 'Fixture ledger', 'HARDWARE', 'Fixture', 'Fixture', '', ?, 0, ?, 0, false)",
                    id,
                    code,
                    code.toLowerCase(java.util.Locale.ROOT),
                    quantity,
                    new BigDecimal("10.00")
            );
        }

        void insertLegacyMovement(String productCode, int quantity, int previousQuantity, int newQuantity) {
            jdbc.update(
                    "insert into stock_movements (timestamp, actor, role, product_code, product_name, type, quantity, previous_quantity, new_quantity, reason) values (?, 'legacy', 'Legacy', ?, 'Fixture', 'LOAD', ?, ?, ?, 'Movimento storico')",
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    productCode,
                    quantity,
                    previousQuantity,
                    newQuantity
            );
        }

        Integer integer(String sql) {
            return jdbc.queryForObject(sql, Integer.class);
        }

        Long longValue(String sql) {
            return jdbc.queryForObject(sql, Long.class);
        }

        String text(String sql) {
            return jdbc.queryForObject(sql, String.class);
        }

        Boolean bool(String sql) {
            return jdbc.queryForObject(sql, Boolean.class);
        }
    }
}
