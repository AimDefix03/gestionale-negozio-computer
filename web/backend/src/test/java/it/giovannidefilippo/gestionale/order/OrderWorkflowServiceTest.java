package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.document.FiscalDocumentRequests;
import it.giovannidefilippo.gestionale.document.FiscalDocumentService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderWorkflowServiceTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FiscalDocumentService documentService;

    @Test
    void confirmedOrderReservesStockAndFulfillmentUnloadsPhysicalStock() {
        String productCode = createProduct(4);

        OrderResponse draft = createOrder(productCode, 2);

        assertThat(draft.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(productService.findByCode(productCode).quantity()).isEqualTo(4);
        assertThat(productService.findByCode(productCode).reservedQuantity()).isZero();
        assertThat(productService.findByCode(productCode).availableQuantity()).isEqualTo(4);

        OrderResponse confirmed = orderService.confirm(draft.code(), actor());

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(productService.findByCode(productCode).quantity()).isEqualTo(4);
        assertThat(productService.findByCode(productCode).reservedQuantity()).isEqualTo(2);
        assertThat(productService.findByCode(productCode).availableQuantity()).isEqualTo(2);

        OrderResponse fulfilled = orderService.fulfill(draft.code(), actor());

        assertThat(fulfilled.status()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(productService.findByCode(productCode).quantity()).isEqualTo(2);
        assertThat(productService.findByCode(productCode).reservedQuantity()).isZero();
        assertThat(productService.findByCode(productCode).availableQuantity()).isEqualTo(2);
    }

    @Test
    void cancelingConfirmedOrderRestoresStock() {
        String productCode = createProduct(5);
        OrderResponse draft = createOrder(productCode, 3);
        orderService.confirm(draft.code(), actor());

        OrderResponse canceled = orderService.cancel(draft.code(), actor());

        assertThat(canceled.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(productService.findByCode(productCode).quantity()).isEqualTo(5);
        assertThat(productService.findByCode(productCode).reservedQuantity()).isZero();
        assertThat(productService.findByCode(productCode).availableQuantity()).isEqualTo(5);
    }

    @Test
    void confirmationUsesSellableAvailability() {
        String productCode = createProduct(3);
        OrderResponse firstDraft = createOrder(productCode, 2);
        OrderResponse secondDraft = createOrder(productCode, 2);
        orderService.confirm(firstDraft.code(), actor());

        assertThat(productService.findByCode(productCode).quantity()).isEqualTo(3);
        assertThat(productService.findByCode(productCode).reservedQuantity()).isEqualTo(2);
        assertThat(productService.findByCode(productCode).availableQuantity()).isEqualTo(1);

        assertThatThrownBy(() -> orderService.confirm(secondDraft.code(), actor()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vendibili insufficienti");
    }

    @Test
    void invalidTransitionsAreRejected() {
        String productCode = createProduct(2);
        OrderResponse draft = createOrder(productCode, 1);

        assertThatThrownBy(() -> orderService.fulfill(draft.code(), actor()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordine confermato");

        orderService.confirm(draft.code(), actor());
        orderService.fulfill(draft.code(), actor());

        assertThatThrownBy(() -> orderService.cancel(draft.code(), actor()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bozza o confermati");
    }

    @Test
    void invoicesRequireFulfilledOrders() {
        String productCode = createProduct(2);
        OrderResponse draft = createOrder(productCode, 1);

        assertThatThrownBy(() -> documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(draft.code()), actor()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("solo dopo aver evaso");

        orderService.confirm(draft.code(), actor());
        orderService.fulfill(draft.code(), actor());

        assertThat(documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(draft.code()), actor()).code())
                .startsWith("FS-");
    }

    private OrderResponse createOrder(String productCode, int quantity) {
        return orderService.create(
                new OrderRequests.CreateOrderRequest(
                        "workflow_customer",
                        PaymentMethod.CARD,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, quantity))
                ),
                "workflow_customer",
                actor()
        );
    }

    private String createProduct(int quantity) {
        String code = "FLOW-" + UUID.randomUUID().toString().substring(0, 8);
        productService.create(new ProductRequest(
                code,
                "Prodotto workflow",
                "Prodotto creato per verificare il ciclo operativo ordine.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda di test",
                "",
                quantity,
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        ));
        return code;
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }
}
