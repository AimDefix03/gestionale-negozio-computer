import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  ApiRequestError,
  clearSessionToken,
  fetchAllPages,
  request,
  requestBlob,
  SessionExpiredError,
  setSessionToken,
  toQueryString
} from './httpClient';
import type { PageResponse } from './types';

function jsonResponse(payload: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers(headers),
    json: vi.fn().mockResolvedValue(payload)
  } as unknown as Response;
}

afterEach(() => {
  clearSessionToken();
  vi.unstubAllGlobals();
});

describe('httpClient', () => {
  it('propaga token di sessione e request id', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ value: 'ok' }, 200, { 'X-Request-Id': 'response-id' }));
    vi.stubGlobal('fetch', fetchMock);
    setSessionToken('session-token');

    await expect(request<{ value: string }>('/api/test')).resolves.toEqual({ value: 'ok' });

    const headers = new Headers(fetchMock.mock.calls[0][1]?.headers);
    expect(headers.get('X-Session-Token')).toBe('session-token');
    expect(headers.get('X-Request-Id')).toBeTruthy();
  });

  it('traduce un 401 in sessione scaduta conservando i dettagli', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      code: 'AUTH_SESSION_EXPIRED',
      message: 'Sessione scaduta.',
      details: ['Riconferma le credenziali.'],
      requestId: 'req-401'
    }, 401)));

    const error = await request('/api/protected').catch((exception) => exception);

    expect(error).toBeInstanceOf(SessionExpiredError);
    expect(error).toMatchObject({ status: 401, code: 'AUTH_SESSION_EXPIRED', requestId: 'req-401' });
    expect((error as Error).message).toContain('Riconferma le credenziali.');
  });

  it('preserva il contratto degli errori applicativi', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      code: 'PRODUCT_IN_USE',
      message: 'Prodotto non eliminabile.',
      details: ['Presente in un ordine confermato.'],
      requestId: 'req-409'
    }, 409)));

    const error = await request('/api/products/GPU-001', { method: 'DELETE' }).catch((exception) => exception);

    expect(error).toBeInstanceOf(ApiRequestError);
    expect(error).toMatchObject({ status: 409, code: 'PRODUCT_IN_USE', requestId: 'req-409' });
  });

  it('scarica un blob mantenendo sessione e nome file UTF-8', async () => {
    const response = jsonResponse({}, 200, { 'Content-Disposition': "attachment; filename*=UTF-8''report-vendite.xlsx" });
    const blob = new Blob(['workbook'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    Object.assign(response, { blob: vi.fn().mockResolvedValue(blob) });
    const fetchMock = vi.fn().mockResolvedValue(response);
    vi.stubGlobal('fetch', fetchMock);
    setSessionToken('report-token');

    await expect(requestBlob('/api/reports/sales/export?format=XLSX')).resolves.toEqual({ blob, filename: 'report-vendite.xlsx' });

    const headers = new Headers(fetchMock.mock.calls[0][1]?.headers);
    expect(headers.get('X-Session-Token')).toBe('report-token');
    expect(headers.get('X-Request-Id')).toBeTruthy();
  });

  it('concatena tutte le pagine senza richieste superflue', async () => {
    const pages: PageResponse<number>[] = [
      { content: [1, 2], page: 0, size: 2, totalElements: 3, totalPages: 2, first: true, last: false },
      { content: [3], page: 1, size: 2, totalElements: 3, totalPages: 2, first: false, last: true }
    ];
    const loader = vi.fn((page: number) => Promise.resolve(pages[page]));

    await expect(fetchAllPages(loader, 2)).resolves.toEqual([1, 2, 3]);
    expect(loader.mock.calls).toEqual([[0, 2], [1, 2]]);
  });

  it('serializza solo i filtri valorizzati', () => {
    expect(toQueryString({ page: 2, q: 'scheda video', type: 'ALL', active: true, empty: '' }))
      .toBe('?page=2&q=scheda+video&active=true');
  });
});
