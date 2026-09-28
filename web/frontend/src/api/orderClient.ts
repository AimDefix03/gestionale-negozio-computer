import { fetchAllPages, request, requestIdempotent, toQueryString } from './httpClient';
import type { CancellationPayload, CreateOrderPayload, FinancialReconciliation, Order, OrderOperationalDetail, OrderQuery, PageResponse, ReceiptPayload, ReturnRefundPayload, ReturnRequestPayload } from './types';

const baseUrl = '/api/orders';

export function fetchOrderPage(query: OrderQuery = {}, signal?: AbortSignal): Promise<PageResponse<Order>> {
  return request<PageResponse<Order>>(`${baseUrl}${toQueryString(query)}`, { signal });
}

export function fetchOrderDetail(orderCode: string, signal?: AbortSignal): Promise<OrderOperationalDetail> {
  return request<OrderOperationalDetail>(`${baseUrl}/${encodeURIComponent(orderCode)}/detail`, { signal });
}

export function fetchOrders(query: OrderQuery = {}): Promise<Order[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchOrderPage({ ...filters, page, size: pageSize }), size);
}

export function fetchFinancialReconciliation(signal?: AbortSignal): Promise<FinancialReconciliation> {
  return request<FinancialReconciliation>('/api/financial-reconciliation', { signal });
}

export function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return requestIdempotent<Order>(baseUrl, 'order-create', payload, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function confirmOrder(orderCode: string): Promise<Order> {
  const url = `${baseUrl}/${encodeURIComponent(orderCode)}/confirm`;
  return requestIdempotent<Order>(url, 'order-confirm', { orderCode }, { method: 'POST' });
}

export function fulfillOrder(orderCode: string): Promise<Order> {
  const url = `${baseUrl}/${encodeURIComponent(orderCode)}/fulfill`;
  return requestIdempotent<Order>(url, 'order-fulfill', { orderCode }, { method: 'POST' });
}

export function cancelOrder(orderCode: string, payload: CancellationPayload): Promise<Order> {
  const url = `${baseUrl}/${encodeURIComponent(orderCode)}/cancel`;
  return requestIdempotent<Order>(url, 'order-cancel', { orderCode, payload }, { method: 'POST', body: JSON.stringify(payload) });
}

export function recordOrderReceipt(orderCode: string, payload: ReceiptPayload): Promise<Order> {
  return operation(`${baseUrl}/${encodeURIComponent(orderCode)}/payments/receipts`, `payment-receipt-${orderCode}`, payload);
}

export function requestOrderReturn(orderCode: string, payload: ReturnRequestPayload): Promise<Order> {
  return operation(`${baseUrl}/${encodeURIComponent(orderCode)}/returns`, `return-request-${orderCode}`, payload);
}

export function approveOrderReturn(orderCode: string, returnCode: string, note = ''): Promise<Order> {
  return returnOperation(orderCode, returnCode, 'approve', { note });
}

export function rejectOrderReturn(orderCode: string, returnCode: string, note: string): Promise<Order> {
  return returnOperation(orderCode, returnCode, 'reject', { note });
}

export function receiveOrderReturn(orderCode: string, returnCode: string): Promise<Order> {
  return returnOperation(orderCode, returnCode, 'receive');
}

export function refundOrderReturn(orderCode: string, returnCode: string, payload: ReturnRefundPayload): Promise<Order> {
  return returnOperation(orderCode, returnCode, 'refund', payload);
}

function returnOperation(orderCode: string, returnCode: string, action: string, payload?: object): Promise<Order> {
  const url = `${baseUrl}/${encodeURIComponent(orderCode)}/returns/${encodeURIComponent(returnCode)}/${action}`;
  return operation(url, `return-${action}-${returnCode}`, payload);
}

function operation(url: string, scope: string, payload?: object): Promise<Order> {
  const intentPayload = payload ?? { operation: scope };
  return requestIdempotent<Order>(url, scope, intentPayload, {
    method: 'POST',
    ...(payload ? { body: JSON.stringify(payload) } : {})
  });
}
