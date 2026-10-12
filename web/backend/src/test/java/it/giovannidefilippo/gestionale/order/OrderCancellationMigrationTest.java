package it.giovannidefilippo.gestionale.order;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCancellationMigrationTest {
    @Test
    void backfillsHistoricalCancellationAndEnforcesOneReversalPerOrder() {
        String database = "order_cancellation_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + database + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway baseline = Flyway.configure().dataSource(dataSource).target("25").load();
        baseline.migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("insert into customer_orders (id, code, customer, timestamp, payment_method, total, status, status_changed_at, customer_code, ownership_status) values (801, 'ORD-HIST-CANCEL', 'Cliente storico', current_timestamp, 'Carta', 100, 'CANCELED', current_timestamp, '', 'UNRESOLVED')");
        jdbc.update("insert into order_payments (id, order_id, method, method_details, status, requested_amount, paid_amount, refunded_amount, currency, created_at, updated_at, version) values (802, 801, 'CARD', '', 'CANCELED', 100, 0, 0, 'EUR', current_timestamp, current_timestamp, 0)");

        Flyway upgrade = Flyway.configure().dataSource(dataSource).target("26").load();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("26");
        assertThat(jdbc.queryForObject("select cancellation_reason from customer_orders where id = 801", String.class))
                .isEqualTo("Annullamento storico antecedente alla tracciatura dettagliata");
        assertThat(jdbc.queryForObject("select canceled_by from customer_orders where id = 801", String.class)).isEqualTo("migration-v26");
        jdbc.update("insert into payment_transactions (code, payment_id, type, amount, reference, reason, return_code, cancellation_order_id, recorded_at, recorded_by, recorded_by_role) values ('PAY-REV-801', 802, 'REVERSAL', 100, 'STORNO-801', 'Storno verificato', '', 801, current_timestamp, 'admin', 'Super admin')");
        jdbc.update("insert into customer_orders (id, code, customer, timestamp, payment_method, total, status, status_changed_at, customer_code, ownership_status) values (803, 'ORD-ACTIVE', 'Cliente attivo', current_timestamp, 'Carta', 50, 'CONFIRMED', current_timestamp, '', 'UNRESOLVED')");

        assertThatThrownBy(() -> jdbc.update("insert into payment_transactions (code, payment_id, type, amount, reference, reason, return_code, cancellation_order_id, recorded_at, recorded_by, recorded_by_role) values ('PAY-REV-802', 802, 'REVERSAL', 100, 'STORNO-802', 'Secondo storno', '', 801, current_timestamp, 'admin', 'Super admin')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into payment_transactions (code, payment_id, type, amount, reference, reason, return_code, cancellation_order_id, recorded_at, recorded_by, recorded_by_role) values ('PAY-REV-803', 802, 'REVERSAL', 100, '', 'Storno senza riferimento', '', null, current_timestamp, 'admin', 'Super admin')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update customer_orders set cancellation_reason = 'Metadato non valido', canceled_at = current_timestamp, canceled_by = 'admin', canceled_by_role = 'Admin' where id = 803"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
