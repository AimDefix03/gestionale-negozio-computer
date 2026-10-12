package it.giovannidefilippo.gestionale.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseReceiptPostingCommand(
        Long supplierOrderId,
        Long supplierOrderReceiptId,
        Long supplierOrderReceiptItemId,
        String supplierOrderCode,
        String supplierReceiptCode,
        String productCode,
        int quantity,
        BigDecimal unitCost,
        String reason,
        LocalDateTime receivedAt,
        String actor,
        String role
) {
}
