package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderCodeSequenceTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Test
    void concurrentOrdersReceiveUniqueSequenceCodes() throws Exception {
        int orders = 8;
        List<String> productCodes = new ArrayList<>();
        for (int index = 0; index < orders; index++) {
            String code = "SEQ-ORD-" + UUID.randomUUID().toString().substring(0, 8);
            productService.create(productRequest(code));
            productCodes.add(code);
        }

        CountDownLatch ready = new CountDownLatch(orders);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(orders);
        try {
            List<Future<OrderResponse>> futures = new ArrayList<>();
            for (int index = 0; index < orders; index++) {
                String productCode = productCodes.get(index);
                String customer = "sequence_customer_" + index;
                futures.add(executor.submit(createOrderTask(productCode, customer, ready, start)));
            }
            await(ready);
            start.countDown();

            List<String> codes = new ArrayList<>();
            for (Future<OrderResponse> future : futures) {
                codes.add(future.get(5, TimeUnit.SECONDS).code());
            }

            Set<String> uniqueCodes = codes.stream().collect(Collectors.toSet());
            assertThat(uniqueCodes).hasSize(orders);
            assertThat(codes).allMatch(code -> code.matches("ORD-\\d{4,}"));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<OrderResponse> createOrderTask(String productCode, String customer, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            await(start);
            return orderService.create(
                    new OrderRequests.CreateOrderRequest(
                            customer,
                            PaymentMethod.CARD,
                            List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                    ),
                    customer,
                    new AuthenticatedUser("admin", UserRole.SUPER_ADMIN)
            );
        };
    }

    private ProductRequest productRequest(String code) {
        return new ProductRequest(
                code,
                "Prodotto sequenza ordine",
                "Prodotto creato per verificare la generazione concorrente dei codici ordine.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda di test",
                "",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        );
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout durante il test concorrente.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test concorrente interrotto.", exception);
        }
    }
}
