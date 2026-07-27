import { fetchAllPages, request, toQueryString } from './httpClient';
import type { PageResponse, Product, ProductLookup, ProductPayload, ProductQuery } from './types';

const baseUrl = '/api/products';

export function fetchProductPage(query: ProductQuery = {}): Promise<PageResponse<Product>> {
  return request<PageResponse<Product>>(`${baseUrl}${toQueryString(query)}`);
}

export function fetchProductLookup(): Promise<ProductLookup[]> {
  return request<ProductLookup[]>(`${baseUrl}/lookup`);
}

export function fetchProducts(query: ProductQuery = {}): Promise<Product[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchProductPage({ ...filters, page, size: pageSize }), size);
}

export function createProduct(payload: ProductPayload): Promise<Product> {
  return request<Product>(baseUrl, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updateProduct(code: string, payload: ProductPayload): Promise<Product> {
  return request<Product>(`${baseUrl}/${encodeURIComponent(code)}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteProduct(code: string): Promise<void> {
  await request<void>(`${baseUrl}/${encodeURIComponent(code)}`, { method: 'DELETE' });
}

export function discontinueProduct(code: string): Promise<Product> {
  return request<Product>(`${baseUrl}/${encodeURIComponent(code)}/discontinue`, { method: 'POST' });
}

export async function deleteProducts(codes: string[]): Promise<void> {
  await request<void>(`${baseUrl}/bulk-delete`, {
    method: 'POST',
    body: JSON.stringify({ codes })
  });
}
