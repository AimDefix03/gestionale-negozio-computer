package it.giovannidefilippo.gestionale.purchase;

import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SupplierOrderSummaryResponse(
        String code,
        Long supplierId,
        String supplierCode,
        String supplierName,
        SupplierOrderStatus status,
        String statusLabel,
        LocalDate expectedDeliveryDate,
        BigDecimal total,
        String currency,
        int orderedQuantity,
        int receivedQuantity,
        OffsetDateTime createdAt
) {
    static SupplierOrderSummaryResponse from(SupplierOrder order) {
        return new SupplierOrderSummaryResponse(
                order.getCode(), order.getSupplierId(), order.getSupplierCodeSnapshot(), order.getSupplierNameSnapshot(),
                order.getStatus(), order.getStatus().getLabel(), order.getExpectedDeliveryDate(), order.getTotal(), order.getCurrency(),
                order.getItems().stream().mapToInt(SupplierOrderItem::getOrderedQuantity).sum(),
                order.getItems().stream().mapToInt(SupplierOrderItem::getReceivedQuantity).sum(),
                BusinessTime.utcOffset(order.getCreatedAt())
        );
    }
}
