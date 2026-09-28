import { fetchAllPages, request, requestIdempotent, toQueryString } from './httpClient';
import type {
  CreatePhysicalInventoryPayload,
  InitialStockPayload,
  InventoryReconciliation,
  MovementQuery,
  PageResponse,
  PhysicalInventoryCountPayload,
  PhysicalInventoryDecisionPayload,
  PhysicalInventoryQuery,
  PhysicalInventorySession,
  StockMovement,
  StockMovementPayload
} from './types';

const baseUrl = '/api/inventory/movements';

export function fetchMovementPage(query: MovementQuery = {}): Promise<PageResponse<StockMovement>> {
  return request<PageResponse<StockMovement>>(`${baseUrl}${toQueryString(query)}`);
}

export function fetchMovements(query: MovementQuery = {}): Promise<StockMovement[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchMovementPage({ ...filters, page, size: pageSize }), size);
}

export function createMovement(payload: StockMovementPayload): Promise<StockMovement> {
  return requestIdempotent<StockMovement>(baseUrl, 'movement', payload, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function createInitialBalance(payload: InitialStockPayload): Promise<StockMovement> {
  return requestIdempotent<StockMovement>('/api/inventory/initial-balance', 'initial-stock', payload, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function fetchInventoryReconciliation(): Promise<InventoryReconciliation> {
  return request<InventoryReconciliation>('/api/inventory/reconciliation');
}

const physicalInventoryUrl = '/api/inventory/counts';

export function fetchPhysicalInventoryPage(query: PhysicalInventoryQuery = {}, signal?: AbortSignal): Promise<PageResponse<PhysicalInventorySession>> {
  return request<PageResponse<PhysicalInventorySession>>(`${physicalInventoryUrl}${toQueryString(query)}`, { signal });
}

export function fetchPhysicalInventory(id: number, signal?: AbortSignal): Promise<PhysicalInventorySession> {
  return request<PhysicalInventorySession>(`${physicalInventoryUrl}/${id}`, { signal });
}

export function createPhysicalInventory(payload: CreatePhysicalInventoryPayload): Promise<PhysicalInventorySession> {
  return requestIdempotent<PhysicalInventorySession>(physicalInventoryUrl, 'physical-inventory-create', payload, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function recordPhysicalInventoryCount(sessionId: number, itemId: number, payload: PhysicalInventoryCountPayload): Promise<PhysicalInventorySession> {
  return request<PhysicalInventorySession>(`${physicalInventoryUrl}/${sessionId}/items/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export function submitPhysicalInventory(id: number): Promise<PhysicalInventorySession> {
  return requestIdempotent<PhysicalInventorySession>(`${physicalInventoryUrl}/${id}/submit`, 'physical-inventory-submit', { id }, { method: 'POST' });
}

export function approvePhysicalInventory(id: number, payload: PhysicalInventoryDecisionPayload): Promise<PhysicalInventorySession> {
  return requestIdempotent<PhysicalInventorySession>(`${physicalInventoryUrl}/${id}/approve`, 'physical-inventory-approve', { id, ...payload }, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function cancelPhysicalInventory(id: number, payload: PhysicalInventoryDecisionPayload): Promise<PhysicalInventorySession> {
  return requestIdempotent<PhysicalInventorySession>(`${physicalInventoryUrl}/${id}/cancel`, 'physical-inventory-cancel', { id, ...payload }, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}
