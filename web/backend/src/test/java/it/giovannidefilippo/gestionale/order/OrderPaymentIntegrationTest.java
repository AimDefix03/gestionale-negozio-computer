package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import jakarta.persistence.EntityManager;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Tag("postgresql")
class OrderPaymentIntegrationTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"CARD", "BANK_TRANSFER", "CASH"})
    void orderPersistsOneStructuredPendingPayment(PaymentMethod method) {
        OrderResponse order = createOrder(method);

        assertThat(order.paymentMethod()).isEqualTo(method.getLabel());
        assertThat(order.payment().id()).isNotNull();
        assertThat(order.payment().method()).isEqualTo(method);
        assertThat(order.payment().methodLabel()).isEqualTo(method.getLabel());
        assertThat(order.payment().status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.payment().statusLabel()).isEqualTo("In attesa");
        assertThat(order.payment().requestedAmount()).isEqualByComparingTo("100.00");
        assertThat(order.payment().paidAmount()).isEqualByComparingTo("0.00");
        assertThat(order.payment().outstandingAmount()).isEqualByComparingTo("100.00");
        assertThat(order.payment().currency()).isEqualTo("EUR");
        assertThat(jdbcTemplate.queryForObject("select count(*) from order_payments where order_id = ?", Long.class, order.id())).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("select method from order_payments where order_id = ?", String.class, order.id())).isEqualTo(method.name());
    }

    @Test
    void cancelingDraftOrderCancelsItsPendingPayment() {
        OrderResponse draft = createOrder(PaymentMethod.CARD);

        OrderResponse canceled = orderService.cancel(draft.code(), cancellation(), actor());
        entityManager.flush();

        assertThat(canceled.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(canceled.payment().status()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(canceled.payment().paidAmount()).isEqualByComparingTo("0.00");
        assertThat(jdbcTemplate.queryForObject("select status from order_payments where order_id = ?", String.class, canceled.id())).isEqualTo("CANCELED");
    }

    @Test
    void migratedOnlyOtherMethodCannotBeSelectedForNewOrder() {
        assertThatThrownBy(() -> createOrder(PaymentMethod.OTHER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non selezionabile");
    }

    @Test
    void recordsPartialAndCompleteReceiptsWithImmutableTransactions() {
        OrderResponse draft = createOrder(PaymentMethod.CARD);
        orderService.confirm(draft.code(), actor());

        OrderResponse partial = orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("40.00"), "POS-001", "Acconto cliente"), actor());
        assertThat(partial.payment().status()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(partial.payment().paidAmount()).isEqualByComparingTo("40.00");
        assertThat(partial.payment().outstandingAmount()).isEqualByComparingTo("60.00");
        assertThat(partial.payment().transactions()).hasSize(1);
        assertThat(partial.payment().transactions().get(0).type()).isEqualTo(PaymentTransactionType.RECEIPT);

        OrderResponse paid = orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("60.00"), "POS-002", "Saldo cliente"), actor());
        assertThat(paid.payment().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(paid.payment().paidAmount()).isEqualByComparingTo("100.00");
        assertThat(paid.payment().outstandingAmount()).isZero();
        assertThat(paid.payment().transactions()).hasSize(2);
    }

    @Test
    void historicalPaymentRequiresExplicitReconciliationBeforeNewReceipts() {
        OrderResponse draft = createOrder(PaymentMethod.CARD);
        orderService.confirm(draft.code(), actor());
        entityManager.flush();
        entityManager.clear();
        jdbcTemplate.update("update order_payments set status = 'UNRECONCILED' where order_id = ?", draft.id());

        OrderResponse ambiguous = orderService.findByCode(draft.code(), actor());
        assertThat(ambiguous.payment().reconciliationRequired()).isTrue();
        assertThat(ambiguous.payment().paidAmount()).isNull();
        assertThat(ambiguous.payment().outstandingAmount()).isNull();
        assertThatThrownBy(() -> orderService.recordReceipt(
                draft.code(),
                new OrderOperationRequests.ReceiptRequest(new BigDecimal("10.00"), "POS-HISTORY", "Incasso non ammesso"),
                actor()
        )).isInstanceOf(IllegalStateException.class).hasMessageContaining("Riconcilia");

        OrderResponse reconciled = orderService.reconcilePayment(
                draft.code(),
                new OrderOperationRequests.PaymentReconciliationRequest(new BigDecimal("40.00"), "ESTRATTO-001", "Saldo verificato su evidenza storica"),
                actor()
        );

        assertThat(reconciled.payment().reconciliationRequired()).isFalse();
        assertThat(reconciled.payment().status()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(reconciled.payment().paidAmount()).isEqualByComparingTo("40.00");
        assertThat(reconciled.payment().outstandingAmount()).isEqualByComparingTo("60.00");
        assertThat(reconciled.payment().reconciledBy()).isEqualTo("admin");
        assertThat(reconciled.payment().reconciliationReference()).isEqualTo("ESTRATTO-001");
        assertThat(reconciled.payment().reconciliationReason()).isEqualTo("Saldo verificato su evidenza storica");
        assertThat(reconciled.payment().transactions()).singleElement()
                .extracting(OrderResponse.PaymentTransactionResponse::type)
                .isEqualTo(PaymentTransactionType.RECONCILIATION);
        assertThatThrownBy(() -> orderService.reconcilePayment(
                draft.code(),
                new OrderOperationRequests.PaymentReconciliationRequest(BigDecimal.ZERO, "", "Seconda riconciliazione"),
                actor()
        )).isInstanceOf(IllegalStateException.class).hasMessageContaining("non richiede");
    }

    @Test
    void rejectsOverpaymentAndReversesReceiptOnCancellation() {
        OrderResponse draft = createOrder(PaymentMethod.CARD);
        orderService.confirm(draft.code(), actor());
        orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("40.00"), "", "Acconto"), actor());

        assertThatThrownBy(() -> orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("61.00"), "", "Importo eccedente"), actor()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supera il saldo residuo");
        OrderResponse canceled = orderService.cancel(
                draft.code(),
                new OrderOperationRequests.CancellationRequest("STORNO-ACCONTO", "Ordine duplicato"),
                actor()
        );

        assertThat(canceled.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(canceled.payment().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(canceled.payment().netPaidAmount()).isZero();
        assertThat(canceled.payment().transactions()).extracting(OrderResponse.PaymentTransactionResponse::type)
                .containsExactly(PaymentTransactionType.RECEIPT, PaymentTransactionType.REVERSAL);
    }

    @Test
    void returnWorkflowRestocksAndSupportsPartialRefunds() {
        OrderResponse draft = createOrder(PaymentMethod.BANK_TRANSFER);
        orderService.confirm(draft.code(), actor());
        orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("100.00"), "TRN-001", "Bonifico ricevuto"), actor());
        OrderResponse fulfilled = orderService.fulfill(draft.code(), actor());
        String productCode = fulfilled.items().get(0).productCode();
        assertThat(productService.requireProduct(productCode).getQuantity()).isEqualTo(1);

        OrderResponse requested = orderService.requestReturn(
                draft.code(),
                new OrderOperationRequests.ReturnRequest("Prodotto non adatto", List.of(new OrderOperationRequests.ReturnItemRequest(productCode, 1))),
                actor()
        );
        String returnCode = requested.returns().get(0).code();
        assertThat(requested.returns().get(0).status()).isEqualTo(OrderReturnStatus.REQUESTED);

        orderService.approveReturn(draft.code(), returnCode, new OrderOperationRequests.ReturnReviewRequest("Verifica completata"), actor());
        OrderResponse received = orderService.receiveReturn(draft.code(), returnCode, actor());
        assertThat(received.returns().get(0).status()).isEqualTo(OrderReturnStatus.RECEIVED);
        assertThat(productService.requireProduct(productCode).getQuantity()).isEqualTo(2);

        OrderResponse partialRefund = orderService.refundReturn(draft.code(), returnCode, new OrderOperationRequests.ReturnRefundRequest(new BigDecimal("30.00"), "REF-001", "Primo rimborso"), actor());
        assertThat(partialRefund.payment().status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(partialRefund.payment().netPaidAmount()).isEqualByComparingTo("70.00");
        assertThat(partialRefund.returns().get(0).status()).isEqualTo(OrderReturnStatus.PARTIALLY_REFUNDED);

        OrderResponse refunded = orderService.refundReturn(draft.code(), returnCode, new OrderOperationRequests.ReturnRefundRequest(new BigDecimal("70.00"), "REF-002", "Saldo rimborso"), actor());
        assertThat(refunded.payment().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.payment().refundedAmount()).isEqualByComparingTo("100.00");
        assertThat(refunded.payment().netPaidAmount()).isZero();
        assertThat(refunded.returns().get(0).status()).isEqualTo(OrderReturnStatus.REFUNDED);
        assertThat(refunded.payment().transactions()).hasSize(3);
    }

    @Test
    void rejectsDuplicateReturnedQuantityAndRefundBeforeReceipt() {
        OrderResponse draft = createOrder(PaymentMethod.CASH);
        orderService.confirm(draft.code(), actor());
        orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("100.00"), "", "Pagamento cassa"), actor());
        OrderResponse fulfilled = orderService.fulfill(draft.code(), actor());
        String productCode = fulfilled.items().get(0).productCode();
        OrderOperationRequests.ReturnRequest request = new OrderOperationRequests.ReturnRequest("Reso", List.of(new OrderOperationRequests.ReturnItemRequest(productCode, 1)));
        OrderResponse requested = orderService.requestReturn(draft.code(), request, actor());
        String returnCode = requested.returns().get(0).code();

        assertThatThrownBy(() -> orderService.requestReturn(draft.code(), request, actor()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Disponibile: 0");
        assertThatThrownBy(() -> orderService.refundReturn(draft.code(), returnCode, new OrderOperationRequests.ReturnRefundRequest(new BigDecimal("10.00"), "", "Rimborso anticipato"), actor()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reso ricevuto");
    }

    @Test
    void supportsMultipleProductsAcrossTwoReturnsWithCumulativeQuantitiesAndIsolatedRefundLedgers() {
        OrderResponse draft = createMultiLineOrder();
        orderService.confirm(draft.code(), actor());
        orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("350.00"), "TRN-MULTI", "Saldo ordine multi-riga"), actor());
        OrderResponse fulfilled = orderService.fulfill(draft.code(), actor());

        OrderResponse firstRequested = orderService.requestReturn(
                draft.code(),
                new OrderOperationRequests.ReturnRequest("Primo reso", List.of(
                        new OrderOperationRequests.ReturnItemRequest(productCode(fulfilled, "GPU"), 1),
                        new OrderOperationRequests.ReturnItemRequest(productCode(fulfilled, "CPU"), 2)
                )),
                actor()
        );
        OrderResponse.OrderReturnResponse firstReturn = firstRequested.returns().get(0);
        assertThat(firstReturn.id()).isNotNull();
        assertThat(item(firstRequested, "GPU").returnedOrReservedQuantity()).isEqualTo(1);
        assertThat(item(firstRequested, "GPU").returnableQuantity()).isEqualTo(1);
        assertThat(item(firstRequested, "CPU").returnedOrReservedQuantity()).isEqualTo(2);
        assertThat(item(firstRequested, "CPU").returnableQuantity()).isEqualTo(1);

        orderService.approveReturn(draft.code(), firstReturn.code(), new OrderOperationRequests.ReturnReviewRequest("Primo reso approvato"), actor());
        orderService.receiveReturn(draft.code(), firstReturn.code(), actor());
        OrderResponse firstRefunded = orderService.refundReturn(
                draft.code(),
                firstReturn.code(),
                new OrderOperationRequests.ReturnRefundRequest(new BigDecimal("50.00"), "REF-FIRST", "Rimborso parziale primo reso"),
                actor()
        );
        OrderResponse.OrderReturnResponse firstAfterRefund = returnByCode(firstRefunded, firstReturn.code());
        assertThat(firstAfterRefund.status()).isEqualTo(OrderReturnStatus.PARTIALLY_REFUNDED);
        assertThat(firstAfterRefund.refundTransactions()).singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.reference()).isEqualTo("REF-FIRST");
                    assertThat(transaction.returnId()).isEqualTo(firstReturn.id());
                });

        OrderResponse secondRequested = orderService.requestReturn(
                draft.code(),
                new OrderOperationRequests.ReturnRequest("Secondo reso", List.of(
                        new OrderOperationRequests.ReturnItemRequest(productCode(fulfilled, "GPU"), 1),
                        new OrderOperationRequests.ReturnItemRequest(productCode(fulfilled, "CPU"), 1)
                )),
                actor()
        );
        OrderResponse.OrderReturnResponse secondReturn = secondRequested.returns().stream()
                .filter(orderReturn -> !orderReturn.code().equals(firstReturn.code()))
                .findFirst()
                .orElseThrow();
        assertThat(item(secondRequested, "GPU").returnedOrReservedQuantity()).isEqualTo(2);
        assertThat(item(secondRequested, "GPU").returnableQuantity()).isZero();
        assertThat(item(secondRequested, "CPU").returnedOrReservedQuantity()).isEqualTo(3);
        assertThat(item(secondRequested, "CPU").returnableQuantity()).isZero();
        assertThat(returnByCode(secondRequested, firstReturn.code()).refundTransactions()).hasSize(1);
        assertThat(secondReturn.refundTransactions()).isEmpty();

        orderService.approveReturn(draft.code(), secondReturn.code(), new OrderOperationRequests.ReturnReviewRequest("Secondo reso approvato"), actor());
        orderService.receiveReturn(draft.code(), secondReturn.code(), actor());
        OrderResponse secondRefunded = orderService.refundReturn(
                draft.code(),
                secondReturn.code(),
                new OrderOperationRequests.ReturnRefundRequest(new BigDecimal("25.00"), "REF-SECOND", "Rimborso parziale secondo reso"),
                actor()
        );

        assertThat(returnByCode(secondRefunded, firstReturn.code()).refundTransactions())
                .extracting(OrderResponse.PaymentTransactionResponse::reference)
                .containsExactly("REF-FIRST");
        assertThat(returnByCode(secondRefunded, secondReturn.code()).refundTransactions())
                .extracting(OrderResponse.PaymentTransactionResponse::reference)
                .containsExactly("REF-SECOND");
        assertThat(secondRefunded.payment().refundedAmount()).isEqualByComparingTo("75.00");
    }

    private OrderResponse createOrder(PaymentMethod method) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String productCode = "PAY-" + suffix;
        productService.create(new ProductRequest(
                productCode,
                "Prodotto pagamento",
                "Prodotto creato per verificare il modello pagamento strutturato.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Componente di test",
                "",
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        ));
        inventoryService.initialBalance(productCode, 2, "Saldo iniziale pagamenti", "test", "Test");
        return orderService.create(
                new OrderRequests.CreateOrderRequest(
                        "cliente_pagamento_" + suffix,
                        method,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                ),
                "cliente_pagamento_" + suffix,
                actor()
        );
    }

    private OrderResponse createMultiLineOrder() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String gpuCode = "GPU-" + suffix;
        String cpuCode = "CPU-" + suffix;
        createProduct(gpuCode, "Scheda video", new BigDecimal("100.00"), 2);
        createProduct(cpuCode, "Processore", new BigDecimal("50.00"), 3);
        return orderService.create(
                new OrderRequests.CreateOrderRequest(
                        "cliente_multi_" + suffix,
                        PaymentMethod.BANK_TRANSFER,
                        List.of(
                                new OrderRequests.CreateOrderItemRequest(gpuCode, 2),
                                new OrderRequests.CreateOrderItemRequest(cpuCode, 3)
                        )
                ),
                "cliente_multi_" + suffix,
                actor()
        );
    }

    private void createProduct(String code, String name, BigDecimal price, int quantity) {
        productService.create(new ProductRequest(
                code,
                name,
                "Prodotto creato per verificare i resi multi-riga.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Componente di test",
                "",
                price,
                BigDecimal.ZERO
        ));
        inventoryService.initialBalance(code, quantity, "Saldo iniziale resi multi-riga", "test", "Test");
    }

    private static String productCode(OrderResponse order, String prefix) {
        return order.items().stream()
                .map(OrderResponse.OrderItemResponse::productCode)
                .filter(code -> code.startsWith(prefix + "-"))
                .findFirst()
                .orElseThrow();
    }

    private static OrderResponse.OrderItemResponse item(OrderResponse order, String prefix) {
        return order.items().stream()
                .filter(orderItem -> orderItem.productCode().startsWith(prefix + "-"))
                .findFirst()
                .orElseThrow();
    }

    private static OrderResponse.OrderReturnResponse returnByCode(OrderResponse order, String returnCode) {
        return order.returns().stream()
                .filter(orderReturn -> orderReturn.code().equals(returnCode))
                .findFirst()
                .orElseThrow();
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }

    private static OrderOperationRequests.CancellationRequest cancellation() {
        return new OrderOperationRequests.CancellationRequest("", "Annullamento richiesto dal test");
    }
}
