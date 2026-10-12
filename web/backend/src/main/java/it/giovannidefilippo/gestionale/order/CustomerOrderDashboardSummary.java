package it.giovannidefilippo.gestionale.order;

import java.util.List;

public record CustomerOrderDashboardSummary(
        long totalOrders,
        long draftOrders,
        long confirmedOrders,
        long fulfilledOrders,
        long canceledOrders,
        List<CustomerOrderSummaryResponse> recentOrders
) {
}
