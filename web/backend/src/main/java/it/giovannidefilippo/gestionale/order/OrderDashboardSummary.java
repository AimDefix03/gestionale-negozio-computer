package it.giovannidefilippo.gestionale.order;

import java.math.BigDecimal;

public record OrderDashboardSummary(
        long totalOrders,
        long draftOrders,
        long confirmedOrders,
        long fulfilledOrders,
        long canceledOrders,
        BigDecimal draftOrderValue,
        BigDecimal confirmedOrderValue,
        BigDecimal fulfilledOrderValue,
        BigDecimal grossCollected,
        BigDecimal refunded,
        BigDecimal netCollected
) {
}
