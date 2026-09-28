package it.giovannidefilippo.gestionale.purchase;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseReceiptInventoryCostMigrationTest {
    @Test
    void preservesHistoricalReceiptsWithoutInventingStockOrCostEvidence() {
        String database = "purchase_cost_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("33").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        jdbc.update("insert into business_partners (id, code, code_canonical, type, display_name, active, created_at, updated_at) values (4101, 'FOR-HIST', 'for-hist', 'SUPPLIER', 'Fornitore storico', true, current_timestamp, current_timestamp)");
        jdbc.update("insert into products (id, version, code, code_canonical, name, description, category, brand, product_type, usage_context, quantity, reserved_quantity, price, discount, discontinued) values (4201, 0, 'PRD-HIST', 'prd-hist', 'Prodotto storico', 'Prodotto con ricezione antecedente alla valorizzazione.', 'HARDWARE', 'Brand', 'Componente', '', 0, 0, 100, 0, false)");
        jdbc.update("insert into supplier_orders (id, version, code, supplier_id, supplier_code_snapshot, supplier_name_snapshot, status, expected_delivery_date, notes, total, currency, created_at, created_by, created_by_role, sent_at) values (4301, 0, 'PO-HIST', 4101, 'FOR-HIST', 'Fornitore storico', 'PARTIALLY_RECEIVED', current_date, '', 80, 'EUR', current_timestamp, 'legacy', 'Dipendente', current_timestamp)");
        jdbc.update("insert into supplier_order_items (id, supplier_order_id, product_id, product_code_snapshot, product_name_snapshot, ordered_quantity, received_quantity, unit_price, line_total, expected_delivery_date) values (4401, 4301, 4201, 'PRD-HIST', 'Prodotto storico', 2, 1, 40, 80, current_date)");
        jdbc.update("insert into supplier_order_receipts (id, code, supplier_order_id, reason, received_at, received_by, received_by_role) values (4501, 'PR-HIST', 4301, 'Ricezione storica', current_timestamp, 'legacy', 'Dipendente')");
        jdbc.update("insert into supplier_order_receipt_items (id, receipt_id, supplier_order_item_id, quantity) values (4601, 4501, 4401, 1)");

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("34").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("34");
        assertThat(jdbc.queryForObject("select inventory_posting_status from supplier_order_receipt_items where id = 4601", String.class)).isEqualTo("LEGACY_UNPOSTED");
        assertThat(jdbc.queryForObject("select expected_unit_cost from supplier_order_receipt_items where id = 4601", BigDecimal.class)).isEqualByComparingTo("40.0000");
        assertThat(jdbc.queryForObject("select actual_unit_cost from supplier_order_receipt_items where id = 4601", BigDecimal.class)).isEqualByComparingTo("40.0000");
        assertThat(jdbc.queryForObject("select total_cost from supplier_order_receipt_items where id = 4601", BigDecimal.class)).isEqualByComparingTo("40.0000");
        assertThat(jdbc.queryForObject("select stock_movement_id from supplier_order_receipt_items where id = 4601", Long.class)).isNull();
        assertThat(jdbc.queryForObject("select costed_quantity from products where id = 4201", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select average_purchase_cost from products where id = 4201", BigDecimal.class)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from stock_movements where supplier_order_receipt_item_id = 4601", Long.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("update supplier_order_receipt_items set inventory_posting_status = 'POSTED' where id = 4601"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
