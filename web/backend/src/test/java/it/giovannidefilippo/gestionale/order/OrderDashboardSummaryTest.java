package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderDashboardSummaryTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Test
    void separatesOrderStatesGrossCollectionsRefundsAndNetCollections() {
        OrderDashboardSummary before = orderService.dashboardSummary();
        String productCode = "KPI-" + UUID.randomUUID().toString().substring(0, 8);
        productService.create(new ProductRequest(
                productCode, "Prodotto KPI", "Prodotto per la matrice economica dashboard.", ProductCategory.HARDWARE,
                "TestBrand", "Componente", "", new BigDecimal("100.00"), BigDecimal.ZERO
        ));
        inventoryService.initialBalance(productCode, 10, "Saldo iniziale KPI", "test", "Test");

        OrderResponse draft = createOrder(productCode, "Cliente bozza");
        OrderResponse confirmed = createOrder(productCode, "Cliente confermato");
        orderService.confirm(confirmed.code(), actor());
        orderService.recordReceipt(confirmed.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("40.00"), "INC-40", "Incasso parziale"), actor());

        OrderResponse fulfilled = createOrder(productCode, "Cliente evaso");
        orderService.confirm(fulfilled.code(), actor());
        orderService.fulfill(fulfilled.code(), actor());
        orderService.recordReceipt(fulfilled.code(), new OrderOperationRequests.ReceiptRequest(new BigDecimal("100.00"), "INC-100", "Incasso completo"), actor());
        OrderResponse withReturn = orderService.requestReturn(fulfilled.code(), new OrderOperationRequests.ReturnRequest(
                "Reso parziale", List.of(new OrderOperationRequests.ReturnItemRequest(productCode, 1))
        ), actor());
        String returnCode = withReturn.returns().get(0).code();
        orderService.approveReturn(fulfilled.code(), returnCode, new OrderOperationRequests.ReturnReviewRequest("Approvato"), actor());
        orderService.receiveReturn(fulfilled.code(), returnCode, actor());
        orderService.refundReturn(fulfilled.code(), returnCode, new OrderOperationRequests.ReturnRefundRequest(new BigDecimal("30.00"), "RIM-30", "Rimborso parziale"), actor());

        OrderResponse canceled = createOrder(productCode, "Cliente annullato");
        orderService.cancel(canceled.code(), new OrderOperationRequests.CancellationRequest("", "Bozza annullata"), actor());

        OrderDashboardSummary after = orderService.dashboardSummary();
        assertThat(after.totalOrders() - before.totalOrders()).isEqualTo(4);
        assertThat(after.draftOrders() - before.draftOrders()).isEqualTo(1);
        assertThat(after.confirmedOrders() - before.confirmedOrders()).isEqualTo(1);
        assertThat(after.fulfilledOrders() - before.fulfilledOrders()).isEqualTo(1);
        assertThat(after.canceledOrders() - before.canceledOrders()).isEqualTo(1);
        assertThat(after.draftOrderValue().subtract(before.draftOrderValue())).isEqualByComparingTo("100.00");
        assertThat(after.confirmedOrderValue().subtract(before.confirmedOrderValue())).isEqualByComparingTo("100.00");
        assertThat(after.fulfilledOrderValue().subtract(before.fulfilledOrderValue())).isEqualByComparingTo("100.00");
        assertThat(after.grossCollected().subtract(before.grossCollected())).isEqualByComparingTo("140.00");
        assertThat(after.refunded().subtract(before.refunded())).isEqualByComparingTo("30.00");
        assertThat(after.netCollected().subtract(before.netCollected())).isEqualByComparingTo("110.00");
        assertThat(draft.status()).isEqualTo(OrderStatus.DRAFT);
    }

    private OrderResponse createOrder(String productCode, String customer) {
        return orderService.create(new OrderRequests.CreateOrderRequest(
                customer, PaymentMethod.CARD, List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
        ), customer, actor());
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("kpi-admin", UserRole.SUPER_ADMIN);
    }
}
