package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.order.CustomerOrderSummaryResponse;

import java.util.List;

public record CustomerDashboardResponse(
        long totalOrders,
        long draftOrders,
        long confirmedOrders,
        long fulfilledOrders,
        long canceledOrders,
        List<CustomerOrderSummaryResponse> recentOrders
) {
}
