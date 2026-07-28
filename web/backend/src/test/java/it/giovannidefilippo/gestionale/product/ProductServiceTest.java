package it.giovannidefilippo.gestionale.product;

import it.giovannidefilippo.gestionale.order.OrderRequests;
import it.giovannidefilippo.gestionale.order.OrderResponse;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.order.PaymentMethod;
import it.giovannidefilippo.gestionale.order.OrderStatus;
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
class ProductServiceTest {
    @Autowired
    private ProductService service;

    @Autowired
    private OrderService orderService;

    @Test
    void createsProductAndCalculatesDiscountedPrice() {
        ProductResponse response = service.create(request("GPU-001", "RTX 5090", "Scheda grafica high-end"));

        assertThat(response.code()).isEqualTo("GPU-001");
        assertThat(response.brand()).isEqualTo("NVIDIA");
        assertThat(response.productType()).isEqualTo("Scheda grafica");
        assertThat(response.reservedQuantity()).isZero();
        assertThat(response.availableQuantity()).isEqualTo(4);
        assertThat(response.discountedPrice()).isEqualByComparingTo("1800.00");
    }

    @Test
    void rejectsDuplicateProductCode() {
        service.create(request("CPU-001", "Ryzen 7 9800X3D", "Processore desktop"));

        assertThatThrownBy(() -> service.create(request("cpu-001", "Altro prodotto", "Duplicato")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codice");
    }

    @Test
    void productWithoutOrderReferencesCanBeRenamed() {
        ProductResponse created = service.create(request(uniqueCode("FREE"), "Prodotto libero", "Prodotto senza ordini collegati"));
        ProductRequest updatedRequest = request(uniqueCode("RENAMED"), "Prodotto rinominato", "Prodotto aggiornabile");

        ProductResponse updated = service.update(created.code(), updatedRequest);

        assertThat(updated.code()).isEqualTo(updatedRequest.code());
        assertThat(updated.name()).isEqualTo("Prodotto rinominato");
    }

    @Test
    void productUsedByOrderCannotBeRenamed() {
        String productCode = uniqueCode("USED");
        service.create(request(productCode, "Prodotto usato", "Prodotto collegato a un ordine"));
        createOrder(productCode);

        assertThatThrownBy(() -> service.update(productCode, request(uniqueCode("NEW"), "Nuovo codice", "Tentativo non valido")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codice prodotto non puo essere modificato");
    }

    @Test
    void productWithReservedStockCannotBeDeleted() {
        String productCode = uniqueCode("RES");
        service.create(request(productCode, "Prodotto riservato", "Prodotto con stock riservato"));
        service.reserveStock(productCode, 1);

        assertThatThrownBy(() -> service.delete(productCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock riservato");
    }

    @Test
    void productUsedByConfirmedOrderCannotBeDeleted() {
        String productCode = uniqueCode("CONF");
        service.create(request(productCode, "Prodotto ordinato", "Prodotto collegato a un ordine confermato"));
        OrderResponse order = createOrder(productCode);
        orderService.confirm(order.code(), actor());

        assertThatThrownBy(() -> service.delete(productCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock riservato");
    }

    @Test
    void productUsedByDraftOrderCannotBeDeleted() {
        String productCode = uniqueCode("DRAFT");
        service.create(request(productCode, "Prodotto in bozza", "Prodotto collegato a un ordine in bozza"));
        createOrder(productCode);

        assertThatThrownBy(() -> service.delete(productCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gia presente");
    }

    @Test
    void discontinuedProductIsNotAvailableForNewPurchasesButConfirmedOrderCanBeFulfilled() {
        String productCode = uniqueCode("DISC");
        service.create(request(productCode, "Prodotto disattivabile", "Prodotto da disattivare"));
        OrderResponse order = createOrder(productCode);
        orderService.confirm(order.code(), actor());

        ProductResponse discontinued = service.discontinue(productCode);

        assertThat(discontinued.discontinued()).isTrue();
        assertThatThrownBy(() -> createOrder(productCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disattivato");

        OrderResponse fulfilled = orderService.fulfill(order.code(), actor());

        assertThat(fulfilled.status()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(service.findByCode(productCode).quantity()).isEqualTo(3);
    }

    private OrderResponse createOrder(String productCode) {
        return orderService.create(
                new OrderRequests.CreateOrderRequest(
                        "cliente_test",
                        PaymentMethod.CARD,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                ),
                "cliente_test",
                actor()
        );
    }

    private ProductRequest request(String code, String name, String description) {
        return new ProductRequest(
                code,
                name,
                description,
                ProductCategory.HARDWARE,
                "NVIDIA",
                "Scheda grafica",
                "Gaming",
                4,
                new BigDecimal("2000.00"),
                new BigDecimal("10.00")
        );
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
