package it.giovannidefilippo.gestionale.order;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalPaymentAndNumberingMigrationTest {
    @Test
    void upgradesAmbiguousPaymentsAndInitializesDocumentCountersFromHistory() {
        String database = "historical_payment_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("21").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        insertOrder(jdbc, 901, "ORD-AMBIGUOUS", "FULFILLED", new BigDecimal("120.00"));
        insertOrder(jdbc, 902, "ORD-EVIDENCED", "FULFILLED", new BigDecimal("80.00"));
        insertPayment(jdbc, 911, 901, "PENDING", new BigDecimal("120.00"), BigDecimal.ZERO);
        insertPayment(jdbc, 912, 902, "PARTIALLY_PAID", new BigDecimal("80.00"), new BigDecimal("30.00"));
        jdbc.update("insert into payment_transactions (id, code, payment_id, type, amount, reference, reason, return_code, recorded_at, recorded_by, recorded_by_role) values (921, 'PAY-EVIDENCED', 912, 'RECEIPT', 30.00, 'REF', 'Incasso documentato', null, current_timestamp, 'fixture', 'EMPLOYEE')");
        insertDocument(jdbc, 931, "FS-2026-0007", "SIMULATED_INVOICE", 2026, 7);
        insertDocument(jdbc, 932, "NC-2026-0003", "SIMULATED_CREDIT_NOTE", 2026, 3);
        jdbc.update("insert into document_number_counters (document_type, fiscal_year, next_value, version) values ('SIMULATED_INVOICE', 2026, 2, 0)");

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("23").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("23");
        assertThat(paymentStatus(jdbc, "ORD-AMBIGUOUS")).isEqualTo("UNRECONCILED");
        assertThat(paymentStatus(jdbc, "ORD-EVIDENCED")).isEqualTo("PARTIALLY_PAID");
        assertThat(counter(jdbc, "SIMULATED_INVOICE", 2026)).isEqualTo(8L);
        assertThat(counter(jdbc, "SIMULATED_CREDIT_NOTE", 2026)).isEqualTo(4L);
    }

    private void insertOrder(JdbcTemplate jdbc, long id, String code, String status, BigDecimal total) {
        jdbc.update(
                "insert into customer_orders (id, code, customer, customer_code, timestamp, payment_method, total, status, status_changed_at, ownership_status) values (?, ?, 'Cliente', '', current_timestamp, 'Carta', ?, ?, current_timestamp, 'UNRESOLVED')",
                id,
                code,
                total,
                status
        );
    }

    private void insertPayment(JdbcTemplate jdbc, long id, long orderId, String status, BigDecimal requested, BigDecimal paid) {
        jdbc.update(
                "insert into order_payments (id, order_id, method, method_details, status, requested_amount, paid_amount, refunded_amount, currency, created_at, updated_at, version) values (?, ?, 'CARD', '', ?, ?, ?, 0, 'EUR', current_timestamp, current_timestamp, 0)",
                id,
                orderId,
                status,
                requested,
                paid
        );
    }

    private void insertDocument(JdbcTemplate jdbc, long id, String code, String type, int fiscalYear, long sequence) {
        jdbc.update(
                """
                insert into fiscal_documents (
                    id, code, type, status, created_at, related_order_code, customer, payment_method,
                    taxable_amount, vat_rate, vat_amount, total_amount, created_by, created_by_role,
                    reason, disclaimer, customer_snapshot_code, customer_snapshot_name,
                    customer_snapshot_tax_code, customer_snapshot_vat_number, customer_snapshot_email,
                    customer_snapshot_phone, customer_snapshot_address, customer_snapshot_city,
                    fiscal_year, sequence_number, document_prefix
                ) values (?, ?, ?, 'ISSUED', current_timestamp, ?, 'Cliente', 'Carta', 10, 0.22, 2.20, 12.20,
                    'fixture', 'EMPLOYEE', 'Fixture', 'SIMULATO', '', 'Cliente', '', '', '', '', '', '', ?, ?, ?)
                """,
                id,
                code,
                type,
                "ORD-DOC-" + id,
                fiscalYear,
                sequence,
                type.equals("SIMULATED_INVOICE") ? "FS" : "NC"
        );
    }

    private String paymentStatus(JdbcTemplate jdbc, String orderCode) {
        return jdbc.queryForObject(
                "select p.status from order_payments p join customer_orders o on o.id = p.order_id where o.code = ?",
                String.class,
                orderCode
        );
    }

    private Long counter(JdbcTemplate jdbc, String type, int fiscalYear) {
        return jdbc.queryForObject(
                "select next_value from document_number_counters where document_type = ? and fiscal_year = ?",
                Long.class,
                type,
                fiscalYear
        );
    }
}
