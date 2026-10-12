package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.inventory.StockMovementResponse;
import it.giovannidefilippo.gestionale.order.OrderResponse;
import it.giovannidefilippo.gestionale.order.OrderDashboardSummary;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long products,
        BigDecimal potentialRetailStockValue,
        BigDecimal knownInventoryCostValue,
        BigDecimal potentialGrossMarginOnCostedStock,
        long costedUnits,
        long uncostedUnits,
        BigDecimal costCoveragePercentage,
        long lowStock,
        long outOfStock,
        OrderDashboardSummary orders,
        List<OrderResponse> recentOrders,
        List<StockMovementResponse> recentMovements
) {
}
