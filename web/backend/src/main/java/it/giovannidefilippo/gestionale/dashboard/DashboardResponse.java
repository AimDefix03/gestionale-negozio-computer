package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.inventory.StockMovementResponse;
import it.giovannidefilippo.gestionale.order.OrderResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long products,
        BigDecimal inventoryValue,
        long lowStock,
        long outOfStock,
        long orders,
        BigDecimal revenue,
        List<OrderResponse> recentOrders,
        List<StockMovementResponse> recentMovements
) {
}
