package it.giovannidefilippo.gestionale.product;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductStockConcurrencyTest {
    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void optimisticLockingPreventsConcurrentStockOverwrite() throws Exception {
        String code = "CONC-" + UUID.randomUUID().toString().substring(0, 8);
        service.create(request(code, 1));

        CountDownLatch loaded = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> task = () -> unloadOneUnitFromStaleSnapshot(code, loaded);
            Future<Boolean> first = executor.submit(task);
            Future<Boolean> second = executor.submit(task);

            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(service.findByCode(code).quantity()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean unloadOneUnitFromStaleSnapshot(String code, CountDownLatch loaded) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Product product = repository.findByCodeForStockAdjustment(code).orElseThrow();
                loaded.countDown();
                await(loaded);
                product.updateQuantity(product.getQuantity() - 1);
                repository.flush();
            });
            return true;
        } catch (RuntimeException exception) {
            if (isOptimisticLockFailure(exception)) {
                return false;
            }
            throw exception;
        }
    }

    private static boolean isOptimisticLockFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof OptimisticLockingFailureException || current instanceof OptimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout durante la simulazione concorrente.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test concorrente interrotto.", exception);
        }
    }

    private ProductRequest request(String code, int quantity) {
        return new ProductRequest(
                code,
                "Prodotto concorrente",
                "Prodotto creato per verificare gli aggiornamenti concorrenti dello stock.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda di test",
                "",
                quantity,
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        );
    }
}
