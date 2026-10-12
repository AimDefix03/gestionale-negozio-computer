package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;

public final class InventoryTestSupport {
    private InventoryTestSupport() {
    }

    public static ProductResponse createProductWithStock(
            ProductService productService,
            InventoryService inventoryService,
            ProductRequest request,
            int quantity
    ) {
        ProductResponse product = productService.create(request);
        inventoryService.initialBalance(product.code(), quantity, "Saldo iniziale fixture", "test", "Test");
        return productService.findByCode(product.code());
    }
}
