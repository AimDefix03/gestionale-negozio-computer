import { fetchAllPages, request, toQueryString } from './httpClient';
import type { CustomerProduct, PageResponse, Product, ProductLookup, ProductOperationalDetail, ProductPayload, ProductQuery } from './types';

const baseUrl = '/api/products';

export function fetchProductPage(query: ProductQuery = {}, signal?: AbortSignal): Promise<PageResponse<Product>> {
  return request<PageResponse<Product>>(`${baseUrl}${toQueryString(query)}`, { signal });
}

export function fetchCustomerProductPage(query: ProductQuery = {}, signal?: AbortSignal): Promise<PageResponse<CustomerProduct>> {
  return request<PageResponse<CustomerProduct>>(`/api/customer/catalog${toQueryString(query)}`, { signal });
}

export function fetchProductLookup(signal?: AbortSignal): Promise<ProductLookup[]> {
  return request<ProductLookup[]>(`${baseUrl}/lookup`, { signal });
}

export function fetchProductDetail(code: string, signal?: AbortSignal): Promise<ProductOperationalDetail> {
  return request<ProductOperationalDetail>(`${baseUrl}/${encodeURIComponent(code)}/detail`, { signal });
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
