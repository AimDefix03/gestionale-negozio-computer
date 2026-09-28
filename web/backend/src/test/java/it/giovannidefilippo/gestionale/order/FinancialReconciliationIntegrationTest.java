package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Tag("postgresql")
class FinancialReconciliationIntegrationTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FinancialReconciliationService reconciliationService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void refundIsLinkedToReturnAndBalancedAgainstBothProjections() {
        ReturnFixture fixture = fulfilledReturn("LINK");

        OrderResponse refunded = orderService.refundReturn(fixture.orderCode(), fixture.returnCode(), refund("30.00", "REF-LINK"), actor("contabile-link"));
        FinancialReconciliationResponse report = reconciliationService.reconcile();
        OrderResponse.PaymentTransactionResponse transaction = refunded.payment().transactions().stream()
                .filter(item -> item.type() == PaymentTransactionType.REFUND)
                .findFirst()
                .orElseThrow();

        assertThat(transaction.returnCode()).isEqualTo(fixture.returnCode());
        assertThat(transaction.returnId()).isNotNull();
        assertThat(refunded.payment().refundedAmount()).isEqualByComparingTo("30.00");
        assertThat(refunded.returns().get(0).refundedAmount()).isEqualByComparingTo("30.00");
        assertThat(report.mismatches()).noneMatch(item -> item.orderCode().equals(fixture.orderCode()));
    }

    @Test
    void detectsPaymentAndReturnDriftWithoutSilentlyRepairingEvidence() {
        ReturnFixture fixture = fulfilledReturn("DRIFT");
        orderService.refundReturn(fixture.orderCode(), fixture.returnCode(), refund("30.00", "REF-DRIFT"), actor("contabile-drift"));
        Long paymentId = jdbc.queryForObject("select p.id from order_payments p join customer_orders o on o.id = p.order_id where o.code = ?", Long.class, fixture.orderCode());
        Long returnId = jdbc.queryForObject("select id from order_returns where code = ?", Long.class, fixture.returnCode());

        jdbc.update("update order_payments set paid_amount = 90 where id = ?", paymentId);
        jdbc.update("update order_returns set refunded_amount = 20 where id = ?", returnId);

        FinancialReconciliationResponse report = reconciliationService.reconcile();

        assertThat(report.balanced()).isFalse();
        assertThat(report.mismatches()).extracting(FinancialReconciliationResponse.FinancialMismatch::type)
                .contains(FinancialMismatchType.PAYMENT_PAID_LEDGER_DRIFT, FinancialMismatchType.RETURN_REFUNDED_LEDGER_DRIFT);
        assertThat(jdbc.queryForObject("select paid_amount from order_payments where id = ?", BigDecimal.class, paymentId)).isEqualByComparingTo("90.00");
        assertThat(jdbc.queryForObject("select refunded_amount from order_returns where id = ?", BigDecimal.class, returnId)).isEqualByComparingTo("20.00");

        jdbc.update("update order_payments set paid_amount = 100 where id = ?", paymentId);
        jdbc.update("update order_returns set refunded_amount = 30 where id = ?", returnId);
    }

    @Test
    void databaseRejectsRefundWithoutRelationalReturnLink() {
        ReturnFixture fixture = fulfilledReturn("SQL");
        Long paymentId = jdbc.queryForObject("select p.id from order_payments p join customer_orders o on o.id = p.order_id where o.code = ?", Long.class, fixture.orderCode());

        assertThatThrownBy(() -> jdbc.update("""
                insert into payment_transactions (
                    code, payment_id, type, amount, reference, reason, return_code,
                    cancellation_order_id, return_id, reconciliation_payment_id,
                    recorded_at, recorded_by, recorded_by_role
                ) values (?, ?, 'REFUND', 10, ?, ?, ?, null, null, null, ?, ?, ?)
                """,
                unique("TX-SQL"), paymentId, "REF-SQL", "Collegamento volutamente assente", fixture.returnCode(),
                Timestamp.valueOf(LocalDateTime.now()), "test", "ADMIN"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constraintFailureRollsBackEarlierFinancialProjectionUpdate() {
        ReturnFixture fixture = fulfilledReturn("ROLLBACK");
        Long paymentId = jdbc.queryForObject("select p.id from order_payments p join customer_orders o on o.id = p.order_id where o.code = ?", Long.class, fixture.orderCode());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            jdbc.update("update order_payments set refunded_amount = 10 where id = ?", paymentId);
            jdbc.update("""
                    insert into payment_transactions (
                        code, payment_id, type, amount, reference, reason, return_code,
                        cancellation_order_id, return_id, reconciliation_payment_id,
                        recorded_at, recorded_by, recorded_by_role
                    ) values (?, ?, 'REFUND', 10, ?, ?, ?, null, null, null, ?, ?, ?)
                    """,
                    unique("TX-ROLLBACK"), paymentId, "REF-ROLLBACK", "Fallimento iniettato", fixture.returnCode(),
                    Timestamp.valueOf(LocalDateTime.now()), "test", "ADMIN"
            );
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject("select count(*) from payment_transactions t join order_payments p on p.id = t.payment_id join customer_orders o on o.id = p.order_id where o.code = ? and t.type = 'REFUND'", Long.class, fixture.orderCode())).isZero();
        assertThat(jdbc.queryForObject("select p.refunded_amount from order_payments p join customer_orders o on o.id = p.order_id where o.code = ?", BigDecimal.class, fixture.orderCode())).isZero();
        assertThat(jdbc.queryForObject("select refunded_amount from order_returns where code = ?", BigDecimal.class, fixture.returnCode())).isZero();
    }

    @Test
    void concurrentRefundsCannotExceedReturnOrCollectedAmount() throws Exception {
        ReturnFixture fixture = fulfilledReturn("RACE");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> refundConcurrently(fixture, "A", ready, start));
            Future<Boolean> second = executor.submit(() -> refundConcurrently(fixture, "B", ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbc.queryForObject("select count(*) from payment_transactions t join order_payments p on p.id = t.payment_id join customer_orders o on o.id = p.order_id where o.code = ? and t.type = 'REFUND'", Long.class, fixture.orderCode())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select p.refunded_amount from order_payments p join customer_orders o on o.id = p.order_id where o.code = ?", BigDecimal.class, fixture.orderCode())).isEqualByComparingTo("70.00");
        assertThat(jdbc.queryForObject("select refunded_amount from order_returns where code = ?", BigDecimal.class, fixture.returnCode())).isEqualByComparingTo("70.00");
    }

    private boolean refundConcurrently(ReturnFixture fixture, String suffix, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            orderService.refundReturn(fixture.orderCode(), fixture.returnCode(), refund("70.00", "REF-RACE-" + suffix), actor("contabile-" + suffix.toLowerCase()));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private ReturnFixture fulfilledReturn(String prefix) {
        String productCode = unique(prefix).toUpperCase();
        productService.create(new ProductRequest(
                productCode,
                "Prodotto " + prefix,
                "Prodotto sintetico per la riconciliazione finanziaria.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Componente",
                "",
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        ));
        inventoryService.initialBalance(productCode, 2, "Saldo iniziale riconciliazione", "test", "Test");
        String customer = unique("cliente");
        OrderResponse draft = orderService.create(
                new OrderRequests.CreateOrderRequest(customer, PaymentMethod.CARD, List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))),
                customer,
                actor("creatore")
        );
        orderService.confirm(draft.code(), actor("confermatore"));
        orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("100.00"), "INC-" + prefix, "Incasso verificato"), actor("contabile"));
        orderService.fulfill(draft.code(), actor("magazziniere"));
        OrderResponse requested = orderService.requestReturn(
                draft.code(),
                new OrderOperationRequests.ReturnRequest("Reso di verifica", List.of(new OrderOperationRequests.ReturnItemRequest(productCode, 1))),
                actor("operatore-resi")
        );
        String returnCode = requested.returns().get(0).code();
        orderService.approveReturn(draft.code(), returnCode, new OrderOperationRequests.ReturnReviewRequest("Approvato"), actor("responsabile-resi"));
        orderService.receiveReturn(draft.code(), returnCode, actor("magazziniere-resi"));
        return new ReturnFixture(draft.code(), returnCode);
    }

    private static OrderOperationRequests.ReturnRefundRequest refund(String amount, String reference) {
        return new OrderOperationRequests.ReturnRefundRequest(new BigDecimal(amount), reference, "Rimborso verificato");
    }

    private static AuthenticatedUser actor(String username) {
        return new AuthenticatedUser(username, UserRole.ADMIN);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record ReturnFixture(String orderCode, String returnCode) {
    }
}
