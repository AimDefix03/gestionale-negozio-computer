import { fetchAllPages, request, toQueryString } from './httpClient';
import type { BusinessPartner, BusinessPartnerPayload, PageResponse, PartnerQuery } from './types';

const baseUrl = '/api/partners';

export function fetchPartnerPage(query: PartnerQuery = {}): Promise<PageResponse<BusinessPartner>> {
  return request<PageResponse<BusinessPartner>>(`${baseUrl}${toQueryString(query)}`);
}

export function fetchPartners(query: PartnerQuery = {}): Promise<BusinessPartner[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchPartnerPage({ ...filters, page, size: pageSize }), size);
}

export function createPartner(payload: BusinessPartnerPayload): Promise<BusinessPartner> {
  return request<BusinessPartner>(baseUrl, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function updatePartner(code: string, payload: BusinessPartnerPayload): Promise<BusinessPartner> {
  return request<BusinessPartner>(`${baseUrl}/${encodeURIComponent(code)}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deactivatePartner(code: string): Promise<void> {
  await request<void>(`${baseUrl}/${encodeURIComponent(code)}`, { method: 'DELETE' });
}
