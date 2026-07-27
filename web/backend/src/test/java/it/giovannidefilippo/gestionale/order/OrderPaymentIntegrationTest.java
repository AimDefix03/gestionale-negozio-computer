package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import jakarta.persistence.EntityManager;
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
class OrderPaymentIntegrationTest {
    @Autowired
    private ProductService productService;

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

        OrderResponse canceled = orderService.cancel(draft.code(), actor());
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
    void rejectsOverpaymentAndCancelAfterReceipt() {
        OrderResponse draft = createOrder(PaymentMethod.CARD);
        orderService.confirm(draft.code(), actor());
        orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("40.00"), "", "Acconto"), actor());

        assertThatThrownBy(() -> orderService.recordReceipt(draft.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("61.00"), "", "Importo eccedente"), actor()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supera il saldo residuo");
        assertThatThrownBy(() -> orderService.cancel(draft.code(), actor()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solo un pagamento in attesa");
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
                2,
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        ));
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

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }
}
