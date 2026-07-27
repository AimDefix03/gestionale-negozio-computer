package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DatabaseBusinessConstraintTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseRejectsInvalidProductNumbers() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into products (code, name, description, category, brand, product_type, usage_context, quantity, price, discount, version, reserved_quantity, discontinued)
                values (?, 'Prodotto test', 'Descrizione test', 'HARDWARE', 'Brand', 'Scheda grafica', '', -1, 100.00, 0.00, 0, 0, false)
                """, uniqueCode("BAD-PROD")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into products (code, name, description, category, brand, product_type, usage_context, quantity, price, discount, version, reserved_quantity, discontinued)
                values (?, 'Prodotto test', 'Descrizione test', 'HARDWARE', 'Brand', 'Scheda grafica', '', 1, 100.00, 101.00, 0, 0, false)
                """, uniqueCode("BAD-DISC")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsBlankRequiredProductFields() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into products (code, name, description, category, brand, product_type, usage_context, quantity, price, discount, version, reserved_quantity, discontinued)
                values ('   ', 'Prodotto test', 'Descrizione test', 'HARDWARE', 'Brand', 'Scheda grafica', '', 1, 100.00, 0.00, 0, 0, false)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidOrderItemNumbers() {
        Long orderId = insertOrder(uniqueCode("ORD-CHECK"));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into order_items (order_id, product_code, product_name, quantity, unit_price, line_total)
                values (?, 'P-001', 'Prodotto test', 0, 100.00, 0.00)
                """, orderId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidStructuredPaymentData() {
        Long invalidMethodOrderId = insertOrder(uniqueCode("ORD-PAY-METHOD"));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into order_payments (order_id, method, method_details, status, requested_amount, paid_amount, currency, created_at, updated_at, version)
                values (?, 'UNSUPPORTED', null, 'PENDING', 100.00, 0.00, 'EUR', current_timestamp, current_timestamp, 0)
                """, invalidMethodOrderId))
                .isInstanceOf(DataIntegrityViolationException.class);

        Long overpaidOrderId = insertOrder(uniqueCode("ORD-PAY-AMOUNT"));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into order_payments (order_id, method, method_details, status, requested_amount, paid_amount, currency, created_at, updated_at, version)
                values (?, 'CARD', null, 'PAID', 100.00, 101.00, 'EUR', current_timestamp, current_timestamp, 0)
                """, overpaidOrderId))
                .isInstanceOf(DataIntegrityViolationException.class);

        Long overRefundedOrderId = insertOrder(uniqueCode("ORD-PAY-REFUND"));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into order_payments (order_id, method, method_details, status, requested_amount, paid_amount, refunded_amount, currency, created_at, updated_at, version)
                values (?, 'CARD', null, 'REFUNDED', 100.00, 50.00, 51.00, 'EUR', current_timestamp, current_timestamp, 0)
                """, overRefundedOrderId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidStockMovementNumbers() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into stock_movements (timestamp, actor, role, product_code, product_name, type, quantity, previous_quantity, new_quantity, reason)
                values (current_timestamp, 'admin', 'Super admin', 'P-001', 'Prodotto test', 'LOAD', 0, 0, 0, 'Test vincolo')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into stock_movements (timestamp, actor, role, product_code, product_name, type, quantity, previous_quantity, new_quantity, reason)
                values (current_timestamp, 'admin', 'Super admin', 'P-001', 'Prodotto test', 'UNLOAD', 1, 0, -1, 'Test vincolo')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidFiscalDocumentAmounts() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into fiscal_documents (code, fiscal_year, sequence_number, type, status, created_at, related_order_code, customer, payment_method, taxable_amount, vat_rate, vat_amount, total_amount, created_by, created_by_role, reason, disclaimer)
                values (?, 2099, ?, 'SIMULATED_INVOICE', 'ISSUED', current_timestamp, ?, 'Cliente test', 'Carta', 100.00, 1.50, 0.00, 100.00, 'admin', 'Super admin', 'Test vincolo', 'DOCUMENTO SIMULATO')
                """, uniqueCode("FS-CHECK"), uniqueSequence(), uniqueCode("ORD-DOC")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into fiscal_documents (code, fiscal_year, sequence_number, type, status, created_at, related_order_code, customer, payment_method, taxable_amount, vat_rate, vat_amount, total_amount, created_by, created_by_role, reason, disclaimer)
                values (?, 2099, ?, 'SIMULATED_INVOICE', 'ISSUED', current_timestamp, ?, 'Cliente test', 'Carta', 100.00, 0.22, 22.00, -1.00, 'admin', 'Super admin', 'Test vincolo', 'DOCUMENTO SIMULATO')
                """, uniqueCode("FS-CHECK"), uniqueSequence(), uniqueCode("ORD-DOC")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidFiscalDocumentLineNumbers() {
        Long documentId = insertDocument(uniqueCode("FS-LINE"), uniqueCode("ORD-LINE"));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into fiscal_document_lines (document_id, product_code, description, quantity, unit_price, line_total)
                values (?, 'P-001', 'Prodotto test', 0, 100.00, 0.00)
                """, documentId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsBlankRequiredBusinessPartnerFields() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into business_partners (code, type, display_name, tax_code, vat_number, email, phone, address, city, notes, active, created_at, updated_at)
                values ('   ', 'CUSTOMER', 'Cliente test', '', '', '', '', '', '', '', true, current_timestamp, current_timestamp)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertOrder(String code) {
        jdbcTemplate.update("""
                insert into customer_orders (code, customer, customer_code, timestamp, payment_method, total, status, status_changed_at)
                values (?, 'Cliente test', '', current_timestamp, 'Carta', 100.00, 'DRAFT', current_timestamp)
                """, code);
        return jdbcTemplate.queryForObject("select id from customer_orders where code = ?", Long.class, code);
    }

    private Long insertDocument(String code, String orderCode) {
        jdbcTemplate.update("""
                insert into fiscal_documents (code, fiscal_year, sequence_number, type, status, created_at, related_order_code, customer, payment_method, taxable_amount, vat_rate, vat_amount, total_amount, created_by, created_by_role, reason, disclaimer)
                values (?, 2099, ?, 'SIMULATED_INVOICE', 'ISSUED', current_timestamp, ?, 'Cliente test', 'Carta', 81.97, 0.22, 18.03, 100.00, 'admin', 'Super admin', 'Test vincolo', 'DOCUMENTO SIMULATO')
                """, code, uniqueSequence(), orderCode);
        return jdbcTemplate.queryForObject("select id from fiscal_documents where code = ?", Long.class, code);
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static long uniqueSequence() {
        return Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1_000_000_000L) + 1;
    }
}
