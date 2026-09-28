package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
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

@SpringBootTest
@Tag("postgresql")
class OrderReturnConcurrencyTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void concurrentReturnRequestsCannotReserveTheSameOrderedQuantityTwice() throws Exception {
        String productCode = unique("RETURN").toUpperCase();
        productService.create(new ProductRequest(
                productCode,
                "Prodotto reso concorrente",
                "Prodotto creato per verificare la serializzazione delle richieste di reso.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Componente",
                "",
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        ));
        inventoryService.initialBalance(productCode, 1, "Saldo iniziale reso concorrente", "test", "Test");
        String customer = unique("cliente");
        OrderResponse draft = orderService.create(
                new OrderRequests.CreateOrderRequest(customer, PaymentMethod.CARD, List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))),
                customer,
                actor("creatore")
        );
        orderService.confirm(draft.code(), actor("confermatore"));
        orderService.fulfill(draft.code(), actor("magazziniere"));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> requestConcurrently(draft.code(), productCode, "A", ready, start));
            Future<Boolean> second = executor.submit(() -> requestConcurrently(draft.code(), productCode, "B", ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        OrderResponse order = orderService.findByCode(draft.code(), actor("verificatore"));
        assertThat(order.returns()).hasSize(1);
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.returnedOrReservedQuantity()).isEqualTo(1);
            assertThat(item.returnableQuantity()).isZero();
        });
        assertThat(jdbc.queryForObject("select count(*) from order_returns where order_id = ?", Long.class, draft.id())).isEqualTo(1L);
    }

    private boolean requestConcurrently(String orderCode, String productCode, String suffix, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            orderService.requestReturn(
                    orderCode,
                    new OrderOperationRequests.ReturnRequest("Reso concorrente " + suffix, List.of(new OrderOperationRequests.ReturnItemRequest(productCode, 1))),
                    actor("operatore-" + suffix.toLowerCase())
            );
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static AuthenticatedUser actor(String username) {
        return new AuthenticatedUser(username, UserRole.SUPER_ADMIN);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
