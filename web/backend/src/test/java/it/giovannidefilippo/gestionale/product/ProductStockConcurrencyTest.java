package it.giovannidefilippo.gestionale.product;

import it.giovannidefilippo.gestionale.inventory.InventoryReconciliationStatus;
import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.inventory.InventoryTestSupport;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("postgresql")
class ProductStockConcurrencyTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private ProductService service;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void concurrentReservationAndAdjustmentPreserveStockInvariantAndLedger() throws Exception {
        String code = "CONC-" + UUID.randomUUID().toString().substring(0, 8);
        InventoryTestSupport.createProductWithStock(service, inventoryService, request(code), 2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> reservation = executor.submit(() -> execute(() -> service.reserveStock(code, 2)));
            Future<Boolean> adjustment = executor.submit(() -> execute(() -> inventoryService.adjust(code, -1, "Rettifica concorrente", "test", "Test")));

            assertThat(List.of(reservation.get(5, TimeUnit.SECONDS), adjustment.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            ProductResponse product = service.findByCode(code);
            assertThat(product.quantity()).isGreaterThanOrEqualTo(product.reservedQuantity());
            assertThat(inventoryService.reconciliation().items())
                    .filteredOn(item -> item.productCode().equals(code))
                    .singleElement()
                    .extracting(item -> item.status())
                    .isEqualTo(InventoryReconciliationStatus.BALANCED);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean execute(Runnable operation) {
        try {
            operation.run();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private ProductRequest request(String code) {
        return new ProductRequest(
                code,
                "Prodotto concorrente",
                "Prodotto creato per verificare gli aggiornamenti concorrenti dello stock.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda di test",
                "",
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        );
    }
}
