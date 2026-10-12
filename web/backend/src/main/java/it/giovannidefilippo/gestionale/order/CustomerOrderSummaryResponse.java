package it.giovannidefilippo.gestionale.order;

import java.math.BigDecimal;
import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.time.OffsetDateTime;

public record CustomerOrderSummaryResponse(
        String code,
        OffsetDateTime timestamp,
        BigDecimal total,
        OrderStatus status,
        String statusLabel,
        PaymentStatus paymentStatus,
        String paymentStatusLabel
) {
    static CustomerOrderSummaryResponse from(CustomerOrder order) {
        return new CustomerOrderSummaryResponse(
                order.getCode(),
                BusinessTime.utcOffset(order.getTimestamp()),
                order.getTotal(),
                order.getStatus(),
                order.getStatus().getLabel(),
                order.getPayment().getStatus(),
                order.getPayment().getStatus().getLabel()
        );
    }
}
