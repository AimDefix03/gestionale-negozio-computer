package it.giovannidefilippo.gestionale.order;

import java.time.LocalDate;
import java.util.List;

public interface SalesReportingUsage {
    List<SalesOrderReportSource> findForReport(LocalDate from, LocalDate to, OrderStatus status, int maxRows);
}
