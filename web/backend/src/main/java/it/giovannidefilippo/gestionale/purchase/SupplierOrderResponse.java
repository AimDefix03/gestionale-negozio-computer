package it.giovannidefilippo.gestionale.purchase;

import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record SupplierOrderResponse(
        Long id,
        String code,
        Long supplierId,
        String supplierCode,
        String supplierName,
        SupplierOrderStatus status,
        String statusLabel,
        LocalDate expectedDeliveryDate,
        String notes,
        BigDecimal total,
        String currency,
        OffsetDateTime createdAt,
        String createdBy,
        String createdByRole,
        OffsetDateTime sentAt,
        OffsetDateTime canceledAt,
        String canceledBy,
        String canceledByRole,
        String cancellationReason,
        List<ItemResponse> items,
        List<ReceiptResponse> receipts,
        Capabilities capabilities
) {
    static SupplierOrderResponse from(SupplierOrder order, Capabilities capabilities) {
        return new SupplierOrderResponse(
                order.getId(), order.getCode(), order.getSupplierId(), order.getSupplierCodeSnapshot(),
                order.getSupplierNameSnapshot(), order.getStatus(), order.getStatus().getLabel(),
                order.getExpectedDeliveryDate(), order.getNotes(), order.getTotal(), order.getCurrency(),
                BusinessTime.utcOffset(order.getCreatedAt()), order.getCreatedBy(), order.getCreatedByRole(),
                BusinessTime.utcOffset(order.getSentAt()), BusinessTime.utcOffset(order.getCanceledAt()),
                order.getCanceledBy(), order.getCanceledByRole(), order.getCancellationReason(),
                order.getItems().stream().map(ItemResponse::from).toList(),
                order.getReceipts().stream().map(ReceiptResponse::from).toList(), capabilities
        );
    }

    public record ItemResponse(
            Long id,
            Long productId,
            String productCode,
            String productName,
            int orderedQuantity,
            int receivedQuantity,
            int remainingQuantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            LocalDate expectedDeliveryDate
    ) {
        static ItemResponse from(SupplierOrderItem item) {
            return new ItemResponse(item.getId(), item.getProductId(), item.getProductCodeSnapshot(), item.getProductNameSnapshot(), item.getOrderedQuantity(), item.getReceivedQuantity(), item.remainingQuantity(), item.getUnitPrice(), item.getLineTotal(), item.getExpectedDeliveryDate());
        }
    }

    public record ReceiptResponse(
            String code,
            String reason,
            OffsetDateTime receivedAt,
            String receivedBy,
            String receivedByRole,
            List<ReceiptItemResponse> items
    ) {
        static ReceiptResponse from(SupplierOrderReceipt receipt) {
            return new ReceiptResponse(receipt.getCode(), receipt.getReason(), BusinessTime.utcOffset(receipt.getReceivedAt()), receipt.getReceivedBy(), receipt.getReceivedByRole(), receipt.getItems().stream().map(ReceiptItemResponse::from).toList());
        }
    }

    public record ReceiptItemResponse(
            Long lineId,
            String productCode,
            int quantity,
            BigDecimal expectedUnitCost,
            BigDecimal actualUnitCost,
            BigDecimal unitCostVariance,
            BigDecimal totalCost,
            String inventoryPostingStatus,
            Long stockMovementId
    ) {
        static ReceiptItemResponse from(SupplierOrderReceiptItem item) {
            return new ReceiptItemResponse(
                    item.getOrderItem().getId(), item.getOrderItem().getProductCodeSnapshot(), item.getQuantity(),
                    item.getExpectedUnitCost(), item.getActualUnitCost(), item.getUnitCostVariance(), item.getTotalCost(),
                    item.getInventoryPostingStatus().name(), item.getStockMovementId()
            );
        }
    }

    public record Capabilities(boolean canSend, boolean canReceive, boolean canCancel) {
        static Capabilities none() { return new Capabilities(false, false, false); }
    }
}
