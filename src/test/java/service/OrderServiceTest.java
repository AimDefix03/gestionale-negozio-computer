package service;

import factory.CategoriaProdotto;
import model.Order;
import model.Prodotto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void creaOrdineScaricaMagazzinoEPersiste() {
        ProductService productService = productService();
        InventoryService inventoryService = inventoryService(productService);
        OrderService orderService = orderService();
        Prodotto prodotto = productService.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 3, 80, 0, CategoriaProdotto.HARDWARE);

        Order order = orderService.createOrder("cliente", List.of(prodotto, prodotto), "Contanti", inventoryService, "cliente", "Cliente");

        OrderService reloadedService = orderService();
        assertTrue(order.code().startsWith("ORD-"));
        assertEquals(1, order.items().size());
        assertEquals(2, order.items().get(0).quantity());
        assertEquals(160, order.total());
        assertEquals(1, productService.getProdottoByCodice("HW-1").getQuantita());
        assertEquals(1, reloadedService.getOrders().size());
    }

    @Test
    void rifiutaOrdineConScorteInsufficienti() {
        ProductService productService = productService();
        InventoryService inventoryService = inventoryService(productService);
        OrderService orderService = orderService();
        Prodotto prodotto = productService.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 1, 80, 0, CategoriaProdotto.HARDWARE);

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(
                "cliente",
                List.of(prodotto, prodotto),
                "Contanti",
                inventoryService,
                "cliente",
                "Cliente"
        ));
    }

    @Test
    void filtraOrdiniPerCliente() {
        ProductService productService = productService();
        InventoryService inventoryService = inventoryService(productService);
        OrderService orderService = orderService();
        Prodotto primo = productService.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 4, 80, 0, CategoriaProdotto.HARDWARE);
        Prodotto secondo = productService.creaEInserisciProdotto("HW-2", "SSD", "Disco", "Samsung", "SSD", "Storage", 4, 120, 0, CategoriaProdotto.HARDWARE);

        orderService.createOrder("cliente", List.of(primo), "Contanti", inventoryService, "cliente", "Cliente");
        orderService.createOrder("altro", List.of(secondo), "Bancomat", inventoryService, "altro", "Cliente");

        assertEquals(1, orderService.getOrdersByCustomer("cliente").size());
        assertEquals("cliente", orderService.getOrdersByCustomer("cliente").get(0).customer());
    }

    private ProductService productService() {
        return new ProductService(tempDir.resolve("prodotti.dat").toString());
    }

    private InventoryService inventoryService(ProductService productService) {
        return new InventoryService(productService, tempDir.resolve("movimenti.dat").toString());
    }

    private OrderService orderService() {
        return new OrderService(tempDir.resolve("ordini.dat").toString());
    }
}
