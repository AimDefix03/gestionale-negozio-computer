import { fetchAllPages, idempotencyHeaders, request, toQueryString } from './httpClient';
import type { MovementQuery, PageResponse, StockMovement, StockMovementPayload } from './types';

const baseUrl = '/api/inventory/movements';

export function fetchMovementPage(query: MovementQuery = {}): Promise<PageResponse<StockMovement>> {
  return request<PageResponse<StockMovement>>(`${baseUrl}${toQueryString(query)}`);
}

export function fetchMovements(query: MovementQuery = {}): Promise<StockMovement[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchMovementPage({ ...filters, page, size: pageSize }), size);
}

export function createMovement(payload: StockMovementPayload): Promise<StockMovement> {
  return request<StockMovement>(baseUrl, {
    method: 'POST',
    headers: idempotencyHeaders('movement'),
    body: JSON.stringify(payload)
  });
}
