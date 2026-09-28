package it.giovannidefilippo.gestionale.order;

import java.math.BigDecimal;
import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.time.OffsetDateTime;

public record ProductOrderHistoryResponse(
        String code,
        String customer,
        OrderStatus status,
        String statusLabel,
        OffsetDateTime timestamp,
        int quantity,
        BigDecimal lineTotal
) {
    static ProductOrderHistoryResponse from(CustomerOrder order, String productCode) {
        OrderItem item = order.getItems().stream()
                .filter(candidate -> candidate.getProductCode().equalsIgnoreCase(productCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("L'ordine non contiene il prodotto richiesto."));
        return new ProductOrderHistoryResponse(
                order.getCode(),
                order.getCustomer(),
                order.getStatus(),
                order.getStatus().getLabel(),
                BusinessTime.utcOffset(order.getTimestamp()),
                item.getQuantity(),
                item.getLineTotal()
        );
    }
}
