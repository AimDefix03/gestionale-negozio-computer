import { fetchAllPages, request, requestIdempotent, toQueryString } from './httpClient';
import type { DocumentQuery, FiscalDocument, PageResponse } from './types';

const baseUrl = '/api/documents';

export function fetchDocumentPage(query: DocumentQuery = {}, signal?: AbortSignal): Promise<PageResponse<FiscalDocument>> {
  return request<PageResponse<FiscalDocument>>(`${baseUrl}${toQueryString(query)}`, { signal });
}

export function fetchDocuments(query: DocumentQuery = {}, signal?: AbortSignal): Promise<FiscalDocument[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchDocumentPage({ ...filters, page, size: pageSize }, signal), size);
}

export function createInvoice(orderCode: string): Promise<FiscalDocument> {
  const payload = { orderCode };
  return requestIdempotent<FiscalDocument>(`${baseUrl}/invoice`, 'invoice', payload, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function createCreditNote(orderCode: string, reason: string): Promise<FiscalDocument> {
  const payload = { orderCode, reason };
  return requestIdempotent<FiscalDocument>(`${baseUrl}/credit-note`, 'credit-note', payload, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}
