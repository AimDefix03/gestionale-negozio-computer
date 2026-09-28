package it.giovannidefilippo.gestionale.inventory;

import java.time.OffsetDateTime;
import java.util.List;

public record InventoryReconciliationReport(
        OffsetDateTime generatedAt,
        long totalProducts,
        long balancedProducts,
        long anomalousProducts,
        long orphanedLegacyMovements,
        List<InventoryReconciliationItem> items
) {
}
