package it.giovannidefilippo.gestionale.product;

import java.util.List;

public interface InventoryReportingUsage {
    List<InventoryProductReportSource> findForReport(
            String query,
            ProductCategory category,
            String stock,
            Boolean discontinued,
            int maxRows
    );
}
