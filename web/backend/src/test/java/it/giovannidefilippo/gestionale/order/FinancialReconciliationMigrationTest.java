package it.giovannidefilippo.gestionale.order;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialReconciliationMigrationTest {
    @Test
    void upgradesRefundLinksAndHistoricalReconciliationLedgerWithoutInventingAmounts() {
        String database = "financial_reconciliation_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure().dataSource(dataSource).target("26").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("insert into customer_orders (id, code, customer, timestamp, payment_method, total, status, status_changed_at, customer_code, ownership_status) values (901, 'ORD-REFUND-901', 'Cliente reso', current_timestamp, 'Carta', 100, 'FULFILLED', current_timestamp, '', 'UNRESOLVED')");
        jdbc.update("insert into order_payments (id, order_id, method, method_details, status, requested_amount, paid_amount, refunded_amount, currency, created_at, updated_at, version) values (902, 901, 'CARD', '', 'PARTIALLY_REFUNDED', 100, 100, 30, 'EUR', current_timestamp, current_timestamp, 0)");
        jdbc.update("insert into order_returns (id, code, order_id, status, reason, total_amount, refunded_amount, requested_at, requested_by, requested_by_role, reviewed_at, reviewed_by, received_at, received_by, updated_at, version) values (903, 'RET-0903', 901, 'PARTIALLY_REFUNDED', 'Reso storico', 100, 30, current_timestamp, 'cliente', 'Customer', current_timestamp, 'admin', current_timestamp, 'admin', current_timestamp, 0)");
        jdbc.update("insert into payment_transactions (id, code, payment_id, type, amount, reference, reason, return_code, cancellation_order_id, recorded_at, recorded_by, recorded_by_role) values (904, 'PAY-REC-904', 902, 'RECEIPT', 100, '', 'Incasso storico', '', null, current_timestamp, 'admin', 'Admin')");
        jdbc.update("insert into payment_transactions (id, code, payment_id, type, amount, reference, reason, return_code, cancellation_order_id, recorded_at, recorded_by, recorded_by_role) values (905, 'PAY-REF-905', 902, 'REFUND', 30, 'REF-ESTERNO', 'Rimborso storico', 'RET-0903', null, current_timestamp, 'admin', 'Admin')");
        jdbc.update("insert into customer_orders (id, code, customer, timestamp, payment_method, total, status, status_changed_at, customer_code, ownership_status) values (906, 'ORD-REC-906', 'Cliente riconciliato', current_timestamp, 'Carta', 100, 'CONFIRMED', current_timestamp, '', 'UNRESOLVED')");
        jdbc.update("insert into order_payments (id, order_id, method, method_details, status, requested_amount, paid_amount, refunded_amount, currency, created_at, updated_at, reconciled_at, reconciled_by, reconciled_by_role, reconciliation_reference, reconciliation_reason, version) values (907, 906, 'CARD', '', 'PARTIALLY_PAID', 100, 40, 0, 'EUR', current_timestamp, current_timestamp, current_timestamp, 'superadmin', 'Super admin', 'ESTRATTO-907', 'Importo verificato', 0)");

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("27").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("27");
        assertThat(jdbc.queryForObject("select return_id from payment_transactions where id = 905", Long.class)).isEqualTo(903L);
        assertThat(jdbc.queryForObject("select return_code from payment_transactions where id = 904", String.class)).isNull();
        assertThat(jdbc.queryForObject("select amount from payment_transactions where payment_id = 907 and type = 'RECONCILIATION'", BigDecimal.class)).isEqualByComparingTo("40.00");
        assertThat(jdbc.queryForObject("select reconciliation_payment_id from payment_transactions where payment_id = 907 and type = 'RECONCILIATION'", Long.class)).isEqualTo(907L);
        assertThat(jdbc.queryForObject("select paid_amount from order_payments where id = 907", BigDecimal.class)).isEqualByComparingTo("40.00");

        assertThatThrownBy(() -> jdbc.update("insert into payment_transactions (code, payment_id, type, amount, reason, return_code, cancellation_order_id, return_id, reconciliation_payment_id, recorded_at, recorded_by, recorded_by_role) values ('PAY-BAD-LINK', 902, 'REFUND', 10, 'Link assente', 'RET-0903', null, null, null, current_timestamp, 'admin', 'Admin')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update order_returns set received_at = null where id = 903"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
