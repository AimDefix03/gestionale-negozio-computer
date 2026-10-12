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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
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
class OrderCancellationIntegrationTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void cancellationOfDepositCreatesFullReversalAndReleasesReservation() {
        String productCode = createProduct("DEP", "100.00");
        OrderResponse order = createConfirmedOrder(productCode);
        orderService.recordReceipt(order.code(), receipt("40.00", "ACCONTO-001"), actor("contabile-acconto"));

        OrderResponse canceled = orderService.cancel(
                order.code(),
                cancellation("STORNO-ACCONTO-001", "Ordine duplicato dal cliente"),
                actor("operatore-annullo")
        );

        assertThat(canceled.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(canceled.payment().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(canceled.payment().paidAmount()).isEqualByComparingTo("40.00");
        assertThat(canceled.payment().refundedAmount()).isEqualByComparingTo("40.00");
        assertThat(canceled.payment().netPaidAmount()).isZero();
        assertThat(canceled.payment().outstandingAmount()).isZero();
        assertThat(canceled.payment().transactions()).extracting(OrderResponse.PaymentTransactionResponse::type)
                .containsExactly(PaymentTransactionType.RECEIPT, PaymentTransactionType.REVERSAL);
        OrderResponse.PaymentTransactionResponse reversal = canceled.payment().transactions().get(1);
        assertThat(reversal.amount()).isEqualByComparingTo("40.00");
        assertThat(reversal.reference()).isEqualTo("STORNO-ACCONTO-001");
        assertThat(reversal.cancellationOrderId()).isEqualTo(order.id());
        assertThat(canceled.cancellationReason()).isEqualTo("Ordine duplicato dal cliente");
        assertThat(canceled.canceledBy()).isEqualTo("operatore-annullo");
        assertThat(productService.findByCode(productCode).reservedQuantity()).isZero();
        assertThat(productService.findByCode(productCode).availableQuantity()).isEqualTo(5);
    }

    @Test
    void cancellationOfPaidOrderCreatesOneReversalForTheFullNetAmount() {
        String productCode = createProduct("FULL", "125.50");
        OrderResponse order = createConfirmedOrder(productCode);
        orderService.recordReceipt(order.code(), receipt("125.50", "SALDO-001"), actor("contabile-saldo"));

        OrderResponse canceled = orderService.cancel(
                order.code(),
                cancellation("STORNO-SALDO-001", "Annullamento autorizzato"),
                actor("responsabile-ordini")
        );

        assertThat(canceled.payment().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(canceled.payment().transactions()).filteredOn(transaction -> transaction.type() == PaymentTransactionType.REVERSAL)
                .singleElement()
                .extracting(OrderResponse.PaymentTransactionResponse::amount)
                .isEqualTo(new BigDecimal("125.50"));
        assertThat(jdbc.queryForObject(
                "select count(*) from payment_transactions where cancellation_order_id = ? and type = 'REVERSAL'",
                Long.class,
                order.id()
        )).isEqualTo(1L);
    }

    @Test
    void paidOrderCannotBeCanceledWithoutAReversalReference() {
        String productCode = createProduct("NO-REF", "75.00");
        OrderResponse order = createConfirmedOrder(productCode);
        orderService.recordReceipt(order.code(), receipt("75.00", "SALDO-NO-REF"), actor("contabile-no-ref"));

        assertThatThrownBy(() -> orderService.cancel(
                order.code(),
                cancellation("", "Richiesta priva di riferimento contabile"),
                actor("operatore-no-ref")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riferimento");

        assertThat(jdbc.queryForObject("select status from customer_orders where id = ?", String.class, order.id())).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("select status from order_payments where order_id = ?", String.class, order.id())).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("select count(*) from payment_transactions where cancellation_order_id = ?", Long.class, order.id())).isZero();
        assertThat(jdbc.queryForObject("select reserved_quantity from products where code = ?", Integer.class, productCode)).isEqualTo(1);
    }

    @Test
    void cancellationRollsBackReversalOrderAndEarlierStockReleaseOnMidpointFailure() {
        String firstProduct = createProduct("ROLL-A", "100.00");
        String secondProduct = createProduct("ROLL-B", "100.00");
        OrderResponse order = createConfirmedOrder(firstProduct, secondProduct);
        orderService.recordReceipt(order.code(), receipt("200.00", "SALDO-ROLLBACK"), actor("contabile-rollback"));
        jdbc.update("update products set reserved_quantity = 0 where code = ?", secondProduct);

        assertThatThrownBy(() -> orderService.cancel(
                order.code(),
                cancellation("STORNO-ROLLBACK", "Verifica rollback atomico"),
                actor("operatore-rollback")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riservato");

        assertThat(jdbc.queryForObject("select status from customer_orders where id = ?", String.class, order.id())).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("select cancellation_reason from customer_orders where id = ?", String.class, order.id())).isNull();
        assertThat(jdbc.queryForObject("select status from order_payments where order_id = ?", String.class, order.id())).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("select refunded_amount from order_payments where order_id = ?", BigDecimal.class, order.id())).isEqualByComparingTo("0.00");
        assertThat(jdbc.queryForObject("select count(*) from payment_transactions where cancellation_order_id = ?", Long.class, order.id())).isZero();
        assertThat(jdbc.queryForObject("select reserved_quantity from products where code = ?", Integer.class, firstProduct)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select reserved_quantity from products where code = ?", Integer.class, secondProduct)).isZero();
    }

    @Test
    void concurrentOperatorsProduceOneCancellationAndOneReversal() throws Exception {
        String productCode = createProduct("RACE", "100.00");
        OrderResponse order = createConfirmedOrder(productCode);
        orderService.recordReceipt(order.code(), receipt("100.00", "SALDO-RACE"), actor("contabile-race"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> cancelConcurrently(order.code(), "A", ready, start));
            Future<Boolean> second = executor.submit(() -> cancelConcurrently(order.code(), "B", ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbc.queryForObject("select status from customer_orders where id = ?", String.class, order.id())).isEqualTo("CANCELED");
        assertThat(jdbc.queryForObject("select count(*) from payment_transactions where cancellation_order_id = ?", Long.class, order.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select reserved_quantity from products where code = ?", Integer.class, productCode)).isZero();
    }

    @Test
    void unreconciledHistoricalPaymentBlocksCancellationWithoutSideEffects() {
        String productCode = createProduct("HISTORY", "100.00");
        OrderResponse order = createConfirmedOrder(productCode);
        jdbc.update("update order_payments set status = 'UNRECONCILED' where order_id = ?", order.id());

        assertThatThrownBy(() -> orderService.cancel(
                order.code(),
                cancellation("", "Annullamento pagamento storico"),
                actor("operatore-storico")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Riconcilia");

        assertThat(jdbc.queryForObject("select status from customer_orders where id = ?", String.class, order.id())).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("select reserved_quantity from products where code = ?", Integer.class, productCode)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from payment_transactions where cancellation_order_id = ?", Long.class, order.id())).isZero();
    }

    private boolean cancelConcurrently(String orderCode, String operator, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            orderService.cancel(
                    orderCode,
                    cancellation("STORNO-RACE-" + operator, "Annullamento concorrente " + operator),
                    actor("operatore-" + operator.toLowerCase())
            );
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private OrderResponse createConfirmedOrder(String... productCodes) {
        String customer = unique("cliente");
        OrderResponse draft = orderService.create(
                new OrderRequests.CreateOrderRequest(
                        customer,
                        PaymentMethod.CARD,
                        java.util.Arrays.stream(productCodes)
                                .map(code -> new OrderRequests.CreateOrderItemRequest(code, 1))
                                .toList()
                ),
                customer,
                actor("creatore-ordine")
        );
        return orderService.confirm(draft.code(), actor("confermatore-ordine"));
    }

    private String createProduct(String prefix, String price) {
        String code = unique(prefix).toUpperCase();
        productService.create(new ProductRequest(
                code,
                "Prodotto " + prefix,
                "Prodotto sintetico per il test di annullamento atomico.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Componente",
                "",
                new BigDecimal(price),
                BigDecimal.ZERO
        ));
        inventoryService.initialBalance(code, 5, "Saldo iniziale annullamento", "test", "Test");
        return code;
    }

    private static OrderOperationRequests.ReceiptRequest receipt(String amount, String reference) {
        return new OrderOperationRequests.ReceiptRequest(new BigDecimal(amount), reference, "Incasso verificato");
    }

    private static OrderOperationRequests.CancellationRequest cancellation(String reference, String reason) {
        return new OrderOperationRequests.CancellationRequest(reference, reason);
    }

    private static AuthenticatedUser actor(String username) {
        return new AuthenticatedUser(username, UserRole.ADMIN);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
