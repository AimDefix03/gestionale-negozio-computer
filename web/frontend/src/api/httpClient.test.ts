import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  ApiRequestError,
  clearSessionToken,
  fetchAllPages,
  parseDownloadFilename,
  request,
  requestBlob,
  requestIdempotent,
  sanitizeFilename,
  saveDownloadedFile,
  SessionExpiredError,
  setSessionToken,
  subscribeToSessionExpiration,
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

  it('notifica centralmente il 401 solo quando esiste una sessione attiva', async () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToSessionExpiration(listener);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ message: 'Credenziali non valide.' }, 401)));

    await request('/api/accounts/login', { method: 'POST' }).catch(() => undefined);
    expect(listener).not.toHaveBeenCalled();

    setSessionToken('active-session');
    await request('/api/protected').catch(() => undefined);
    expect(listener).toHaveBeenCalledOnce();
    expect(listener.mock.calls[0][0]).toBeInstanceOf(SessionExpiredError);
    unsubscribe();
  });

  it('propaga AbortSignal alla richiesta fetch', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ value: 'ok' }));
    vi.stubGlobal('fetch', fetchMock);
    const controller = new AbortController();

    await request('/api/accounts', { signal: controller.signal });

    expect(fetchMock.mock.calls[0][1]?.signal).toBe(controller.signal);
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

  it('usa la stessa chiave per due invii concorrenti dello stesso intento', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 42 }));
    vi.stubGlobal('fetch', fetchMock);
    const payload = { customer: 'Cliente', items: [{ productCode: 'GPU-1', quantity: 1 }] };

    const [first, second] = await Promise.all([
      requestIdempotent<{ id: number }>('/api/orders', 'order-create', payload, { method: 'POST', body: JSON.stringify(payload) }),
      requestIdempotent<{ id: number }>('/api/orders', 'order-create', payload, { method: 'POST', body: JSON.stringify(payload) })
    ]);

    expect(first).toEqual({ id: 42 });
    expect(second).toEqual({ id: 42 });
    expect(idempotencyKey(fetchMock, 0)).toBe(idempotencyKey(fetchMock, 1));
  });

  it('riusa la chiave dopo una risposta persa o un timeout ambiguo', async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new DOMException('Timeout', 'AbortError'))
      .mockResolvedValueOnce(jsonResponse({ id: 7 }));
    vi.stubGlobal('fetch', fetchMock);
    const payload = { orderCode: 'ORD-7' };

    await expect(requestIdempotent('/api/orders/ORD-7/confirm', 'order-confirm', payload, { method: 'POST' }))
      .rejects.toMatchObject({ name: 'AbortError' });
    await expect(requestIdempotent('/api/orders/ORD-7/confirm', 'order-confirm', payload, { method: 'POST' }))
      .resolves.toEqual({ id: 7 });

    expect(idempotencyKey(fetchMock, 0)).toBe(idempotencyKey(fetchMock, 1));
  });

  it('genera una nuova chiave solo dopo un esito confermato', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 9 }));
    vi.stubGlobal('fetch', fetchMock);
    const payload = { orderCode: 'ORD-9' };

    await requestIdempotent('/api/orders/ORD-9/fulfill', 'order-fulfill', payload, { method: 'POST' });
    await requestIdempotent('/api/orders/ORD-9/fulfill', 'order-fulfill', payload, { method: 'POST' });

    expect(idempotencyKey(fetchMock, 0)).not.toBe(idempotencyKey(fetchMock, 1));
  });

  it('separa gli intenti quando il payload cambia', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new TypeError('Network unavailable'));
    vi.stubGlobal('fetch', fetchMock);

    await requestIdempotent('/api/inventory/adjustments', 'inventory-adjustment', { quantity: 1 }, { method: 'POST' }).catch(() => undefined);
    await requestIdempotent('/api/inventory/adjustments', 'inventory-adjustment', { quantity: 2 }, { method: 'POST' }).catch(() => undefined);

    expect(idempotencyKey(fetchMock, 0)).not.toBe(idempotencyKey(fetchMock, 1));
  });

  it('mantiene la chiave quando il server segnala che il claim e ancora in corso', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ code: 'IDEMPOTENCY_IN_PROGRESS', message: 'Operazione in corso.' }, 409))
      .mockResolvedValueOnce(jsonResponse({ id: 11 }));
    vi.stubGlobal('fetch', fetchMock);
    const payload = { orderCode: 'ORD-11' };

    await expect(requestIdempotent('/api/orders/ORD-11/confirm', 'order-confirm', payload, { method: 'POST' }))
      .rejects.toMatchObject({ code: 'IDEMPOTENCY_IN_PROGRESS' });
    await requestIdempotent('/api/orders/ORD-11/confirm', 'order-confirm', payload, { method: 'POST' });

    expect(idempotencyKey(fetchMock, 0)).toBe(idempotencyKey(fetchMock, 1));
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

  it('interpreta filename RFC 5987, quoted e fallback malformati senza accettare path', () => {
    expect(parseDownloadFilename("attachment; filename*=UTF-8'it'report%20vendite.csv"))
      .toBe('report vendite.csv');
    expect(parseDownloadFilename('attachment; filename="report \\"Q2\\".xlsx"'))
      .toBe('report _Q2_.xlsx');
    expect(parseDownloadFilename('attachment; filename="../../segreti.csv"'))
      .toBe('_.._segreti.csv');
    expect(parseDownloadFilename("attachment; filename*=UTF-8''%E0%A4%A", 'report.csv'))
      .toBe('report.csv');
    expect(parseDownloadFilename(null, 'report.pdf')).toBe('report.pdf');
    expect(sanitizeFilename('\u0000../')).toBe('_');
  });

  it('collega temporaneamente il link prima del click e revoca il blob dopo il tick per WebKit', () => {
    vi.useFakeTimers();
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    const createObjectURL = vi.fn().mockReturnValue('blob:report');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL });

    saveDownloadedFile({ blob: new Blob(['report']), filename: '../report.csv' });

    expect(click).toHaveBeenCalledOnce();
    expect(document.querySelector('a[download]')).toBeNull();
    expect(revokeObjectURL).not.toHaveBeenCalled();
    vi.runAllTimers();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:report');
    vi.useRealTimers();
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

function idempotencyKey(fetchMock: ReturnType<typeof vi.fn>, callIndex: number): string | null {
  return new Headers(fetchMock.mock.calls[callIndex][1]?.headers).get('Idempotency-Key');
}
