package it.giovannidefilippo.gestionale.order;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesReportingUsage {
    List<SalesOrderReportSource> findForReport(LocalDateTime startInclusiveUtc, LocalDateTime endExclusiveUtc, OrderStatus status, int maxRows);
}
