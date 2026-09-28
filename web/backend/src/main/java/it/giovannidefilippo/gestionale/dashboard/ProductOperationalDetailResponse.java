package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.inventory.StockMovementResponse;
import it.giovannidefilippo.gestionale.order.ProductOrderHistoryResponse;
import it.giovannidefilippo.gestionale.product.ProductResponse;

import java.util.List;

public record ProductOperationalDetailResponse(
        ProductResponse product,
        List<StockMovementResponse> recentMovements,
        List<ProductOrderHistoryResponse> recentOrders
) {
}
