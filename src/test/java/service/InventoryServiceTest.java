package service;

import factory.CategoriaProdotto;
import model.StockMovement;
import model.StockMovementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void registraCaricoEAggiornaQuantitaProdotto() {
        ProductService productService = productService();
        productService.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 2, 80, 0, CategoriaProdotto.HARDWARE);
        InventoryService inventoryService = inventoryService(productService);

        StockMovement movement = inventoryService.registerMovement("HW-1", StockMovementType.CARICO, 5, "Rifornimento", "admin", "Admin");

        assertEquals(7, productService.getProdottoByCodice("HW-1").getQuantita());
        assertEquals(2, movement.previousQuantity());
        assertEquals(7, movement.newQuantity());
    }

    @Test
    void registraScaricoEAggiornaQuantitaProdotto() {
        ProductService productService = productService();
        productService.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 5, 80, 0, CategoriaProdotto.HARDWARE);
        InventoryService inventoryService = inventoryService(productService);

        inventoryService.registerMovement("HW-1", StockMovementType.SCARICO, 3, "Vendita", "dipendente", "Dipendente");

        assertEquals(2, productService.getProdottoByCodice("HW-1").getQuantita());
    }

    @Test
    void rifiutaScaricoSuperioreAllaQuantitaDisponibile() {
        ProductService productService = productService();
        productService.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 2, 80, 0, CategoriaProdotto.HARDWARE);
        InventoryService inventoryService = inventoryService(productService);

        assertThrows(IllegalArgumentException.class, () -> inventoryService.registerMovement(
                "HW-1",
                StockMovementType.SCARICO,
                3,
                "Vendita",
                "admin",
                "Admin"
        ));
    }

    @Test
    void restituisceProdottiSottoSoglia() {
        ProductService productService = productService();
        productService.creaEInserisciProdotto("LOW", "SSD", "Disco", "Samsung", "SSD", "Storage", 3, 80, 0, CategoriaProdotto.HARDWARE);
        productService.creaEInserisciProdotto("OK", "Monitor", "Schermo", "LG", "Monitor", "Office", 8, 180, 0, CategoriaProdotto.HARDWARE);
        InventoryService inventoryService = inventoryService(productService);

        assertEquals(1, inventoryService.getLowStockProducts().size());
        assertEquals("LOW", inventoryService.getLowStockProducts().get(0).getCodice());
    }

    private ProductService productService() {
        return new ProductService(tempDir.resolve("prodotti.dat").toString());
    }

    private InventoryService inventoryService(ProductService productService) {
        return new InventoryService(productService, tempDir.resolve("movimenti.dat").toString());
    }
}
