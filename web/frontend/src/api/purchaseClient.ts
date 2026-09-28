import { request, requestIdempotent, toQueryString } from './httpClient';
import type { CreateSupplierOrderPayload, PageResponse, ReceiveSupplierOrderPayload, SupplierOrder, SupplierOrderQuery, SupplierOrderSummary } from './types';

const baseUrl = '/api/purchase-orders';

export function fetchSupplierOrderPage(query: SupplierOrderQuery = {}, signal?: AbortSignal): Promise<PageResponse<SupplierOrderSummary>> {
  return request<PageResponse<SupplierOrderSummary>>(`${baseUrl}${toQueryString(query)}`, { signal });
}

export function fetchSupplierOrder(code: string, signal?: AbortSignal): Promise<SupplierOrder> {
  return request<SupplierOrder>(`${baseUrl}/${encodeURIComponent(code)}`, { signal });
}

export function createSupplierOrder(payload: CreateSupplierOrderPayload): Promise<SupplierOrder> {
  return requestIdempotent<SupplierOrder>(baseUrl, 'supplier-order-create', payload, { method: 'POST', body: JSON.stringify(payload) });
}

export function sendSupplierOrder(code: string): Promise<SupplierOrder> {
  const url = `${baseUrl}/${encodeURIComponent(code)}/send`;
  return requestIdempotent<SupplierOrder>(url, `supplier-order-send-${code}`, { code }, { method: 'POST' });
}

export function receiveSupplierOrder(code: string, payload: ReceiveSupplierOrderPayload): Promise<SupplierOrder> {
  const url = `${baseUrl}/${encodeURIComponent(code)}/receipts`;
  return requestIdempotent<SupplierOrder>(url, `supplier-order-receive-${code}`, { code, payload }, { method: 'POST', body: JSON.stringify(payload) });
}

export function cancelSupplierOrder(code: string, reason: string): Promise<SupplierOrder> {
  const url = `${baseUrl}/${encodeURIComponent(code)}/cancel`;
  const payload = { reason };
  return requestIdempotent<SupplierOrder>(url, `supplier-order-cancel-${code}`, { code, payload }, { method: 'POST', body: JSON.stringify(payload) });
}
