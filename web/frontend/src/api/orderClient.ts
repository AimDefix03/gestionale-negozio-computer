import { fetchAllPages, idempotencyHeaders, request, toQueryString } from './httpClient';
import type { CreateOrderPayload, Order, OrderQuery, PageResponse, ReceiptPayload, ReturnRefundPayload, ReturnRequestPayload } from './types';

const baseUrl = '/api/orders';

export function fetchOrderPage(query: OrderQuery = {}): Promise<PageResponse<Order>> {
  return request<PageResponse<Order>>(`${baseUrl}${toQueryString(query)}`);
}

export function fetchOrders(query: OrderQuery = {}): Promise<Order[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchOrderPage({ ...filters, page, size: pageSize }), size);
}

export function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return request<Order>(baseUrl, {
    method: 'POST',
    headers: idempotencyHeaders('order-create'),
    body: JSON.stringify(payload)
  });
}

export function confirmOrder(orderCode: string): Promise<Order> {
  return request<Order>(`${baseUrl}/${encodeURIComponent(orderCode)}/confirm`, {
    method: 'POST',
    headers: idempotencyHeaders(`order-confirm-${orderCode}`)
  });
}

export function fulfillOrder(orderCode: string): Promise<Order> {
  return request<Order>(`${baseUrl}/${encodeURIComponent(orderCode)}/fulfill`, {
    method: 'POST',
    headers: idempotencyHeaders(`order-fulfill-${orderCode}`)
  });
}

export function cancelOrder(orderCode: string): Promise<Order> {
  return request<Order>(`${baseUrl}/${encodeURIComponent(orderCode)}/cancel`, {
    method: 'POST',
    headers: idempotencyHeaders(`order-cancel-${orderCode}`)
  });
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
  return request<Order>(url, {
    method: 'POST',
    headers: idempotencyHeaders(scope),
    ...(payload ? { body: JSON.stringify(payload) } : {})
  });
}
