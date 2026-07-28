import { request, requestBlob, saveDownloadedFile, toQueryString } from './httpClient';
import type { InventoryReport, InventoryReportQuery, ReportFormat, SalesReport, SalesReportQuery } from './types';

export function fetchSalesReport(query: SalesReportQuery): Promise<SalesReport> {
  return request<SalesReport>(`/api/reports/sales${toQueryString(query)}`);
}

export function fetchInventoryReport(query: InventoryReportQuery): Promise<InventoryReport> {
  return request<InventoryReport>(`/api/reports/inventory${toQueryString(query)}`);
}

export async function downloadSalesReport(query: SalesReportQuery, format: ReportFormat): Promise<void> {
  const file = await requestBlob(`/api/reports/sales/export${toQueryString({ ...query, format })}`);
  saveDownloadedFile(file);
}

export async function downloadInventoryReport(query: InventoryReportQuery, format: ReportFormat): Promise<void> {
  const file = await requestBlob(`/api/reports/inventory/export${toQueryString({ ...query, format })}`);
  saveDownloadedFile(file);
}
