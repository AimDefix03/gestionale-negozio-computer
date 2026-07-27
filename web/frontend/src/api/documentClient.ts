import { fetchAllPages, idempotencyHeaders, request, toQueryString } from './httpClient';
import type { DocumentQuery, FiscalDocument, PageResponse } from './types';

const baseUrl = '/api/documents';

export function fetchDocumentPage(query: DocumentQuery = {}): Promise<PageResponse<FiscalDocument>> {
  return request<PageResponse<FiscalDocument>>(`${baseUrl}${toQueryString(query)}`);
}

export function fetchDocuments(query: DocumentQuery = {}): Promise<FiscalDocument[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchDocumentPage({ ...filters, page, size: pageSize }), size);
}

export function createInvoice(orderCode: string): Promise<FiscalDocument> {
  return request<FiscalDocument>(`${baseUrl}/invoice`, {
    method: 'POST',
    headers: idempotencyHeaders(`invoice-${orderCode}`),
    body: JSON.stringify({ orderCode })
  });
}

export function createCreditNote(orderCode: string, reason: string): Promise<FiscalDocument> {
  return request<FiscalDocument>(`${baseUrl}/credit-note`, {
    method: 'POST',
    headers: idempotencyHeaders(`credit-note-${orderCode}`),
    body: JSON.stringify({ orderCode, reason })
  });
}
