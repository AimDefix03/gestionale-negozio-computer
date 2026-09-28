package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
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
@Tag("postgresql")
class InventoryServiceTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void registerMovementAdjustsStockAndKeepsMovementSnapshot() {
        String code = "MOV-" + UUID.randomUUID().toString().substring(0, 8);
        createProductWithStock(code, 4);

        StockMovementResponse response = inventoryService.register(code, StockMovementType.UNLOAD, 2, "Vendita banco", "admin", "Admin");

        assertThat(response.previousQuantity()).isEqualTo(4);
        assertThat(response.newQuantity()).isEqualTo(2);
        assertThat(response.deltaQuantity()).isEqualTo(-2);
        assertThat(response.origin()).isEqualTo(StockMovementOrigin.MANUAL_MOVEMENT);
        assertThat(response.authoritative()).isTrue();
        assertThat(productService.findByCode(code).quantity()).isEqualTo(2);
    }

    @Test
    void registerMovementRejectsUnloadOverAvailableStock() {
        String code = "MOV-" + UUID.randomUUID().toString().substring(0, 8);
        createProductWithStock(code, 1);

        assertThatThrownBy(() -> inventoryService.register(code, StockMovementType.UNLOAD, 2, "Scarico non valido", "admin", "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantità disponibile");
        assertThat(productService.findByCode(code).quantity()).isEqualTo(1);
    }

    @Test
    void registerMovementCannotConsumeReservedStock() {
        String code = "MOV-" + UUID.randomUUID().toString().substring(0, 8);
        createProductWithStock(code, 4);
        productService.reserveStock(code, 3);

        assertThatThrownBy(() -> inventoryService.register(code, StockMovementType.UNLOAD, 2, "Scarico con stock riservato", "admin", "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disponibilità vendibile");

        assertThat(productService.findByCode(code).quantity()).isEqualTo(4);
        assertThat(productService.findByCode(code).reservedQuantity()).isEqualTo(3);
        assertThat(productService.findByCode(code).availableQuantity()).isEqualTo(1);
    }

    @Test
    void lowStockProductsUseSellableAvailabilityAndExcludeOutOfStockProducts() {
        String lowCode = "LOW-" + UUID.randomUUID().toString().substring(0, 8);
        String reservedLowCode = "LWR-" + UUID.randomUUID().toString().substring(0, 8);
        String outCode = "OUT-" + UUID.randomUUID().toString().substring(0, 8);
        String availableCode = "AVL-" + UUID.randomUUID().toString().substring(0, 8);
        createProductWithStock(lowCode, 2);
        createProductWithStock(reservedLowCode, 5);
        createProductWithStock(outCode, 0);
        createProductWithStock(availableCode, 6);
        productService.reserveStock(reservedLowCode, 3);

        List<String> codes = inventoryService.lowStockProducts().stream()
                .map(ProductResponse::code)
                .toList();

        assertThat(codes).contains(lowCode, reservedLowCode);
        assertThat(codes).doesNotContain(outCode, availableCode);
        assertThat(productService.countLowStockProducts(InventoryService.LOW_STOCK_THRESHOLD)).isGreaterThanOrEqualTo(2);
    }

    @Test
    void directAdjustmentsAreRejectedToPreserveTheApprovalWorkflow() {
        String code = "LED-" + UUID.randomUUID().toString().substring(0, 8);
        createProductWithStock(code, 7);

        assertThatThrownBy(() -> inventoryService.adjust(code, 1, "Rettifica diretta", "admin", "Admin"))
                .isInstanceOf(it.giovannidefilippo.gestionale.common.ResourceConflictException.class)
                .hasMessageContaining("sessione di conteggio approvata");
        assertThat(productService.findByCode(code).quantity()).isEqualTo(7);
    }

    @Test
    void initialBalanceCanBeRegisteredOnlyOnce() {
        String code = "BASE-" + UUID.randomUUID().toString().substring(0, 8);
        createProductWithStock(code, 2);

        assertThatThrownBy(() -> inventoryService.initialBalance(code, 4, "Secondo saldo", "admin", "Admin"))
                .isInstanceOf(it.giovannidefilippo.gestionale.common.ResourceConflictException.class)
                .hasMessageContaining("già stato registrato");
        assertThat(productService.findByCode(code).quantity()).isEqualTo(2);
    }

    @Test
    void directAdjustmentsAreRejectedEvenWithoutAnInitialBalance() {
        String code = "NOBASE-" + UUID.randomUUID().toString().substring(0, 8);
        productService.create(request(code));

        assertThatThrownBy(() -> inventoryService.adjust(code, 1, "Rettifica prematura", "admin", "Admin"))
                .isInstanceOf(it.giovannidefilippo.gestionale.common.ResourceConflictException.class)
                .hasMessageContaining("sessione di conteggio approvata");
    }

    private void createProductWithStock(String code, int quantity) {
        InventoryTestSupport.createProductWithStock(productService, inventoryService, request(code), quantity);
    }

    private ProductRequest request(String code) {
        return new ProductRequest(
                code,
                "Prodotto magazzino",
                "Prodotto creato per verificare i movimenti di magazzino.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda di test",
                "",
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        );
    }
}
