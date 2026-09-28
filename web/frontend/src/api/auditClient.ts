import { fetchAllPages, request, toQueryString } from './httpClient';
import type { AuditEvent, AuditQuery, PageResponse } from './types';

const baseUrl = '/api/audit';

export function fetchAuditEventPage(query: AuditQuery = {}, signal?: AbortSignal): Promise<PageResponse<AuditEvent>> {
  return request<PageResponse<AuditEvent>>(`${baseUrl}${toQueryString(query)}`, { signal });
}

export function fetchAuditEvents(query: AuditQuery = {}, signal?: AbortSignal): Promise<AuditEvent[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchAuditEventPage({ ...filters, page, size: pageSize }, signal), size);
}
