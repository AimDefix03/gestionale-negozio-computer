import { afterEach, describe, expect, it, vi } from 'vitest';
import { login, register, renewSession } from './accountClient';
import { ApiRequestError, clearSessionToken, setSessionToken } from './httpClient';
import { confirmOrder, createOrder } from './orderClient';
import { createProduct, deleteProduct } from './productClient';

function jsonResponse(payload: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'X-Request-Id': `req-${status}` }),
    json: vi.fn().mockResolvedValue(payload)
  } as unknown as Response;
}

afterEach(() => {
  clearSessionToken();
  vi.unstubAllGlobals();
});

describe('client API di dominio', () => {
  it('invia login e registrazione ai relativi endpoint', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ token: 'token', expiresAt: '2026-07-13T12:00:00Z', user: {} }))
      .mockResolvedValueOnce(jsonResponse({ id: 2, username: 'dipendente' }));
    vi.stubGlobal('fetch', fetchMock);

    await login('superadmin', 'SecurePassword123!', 'SUPER_ADMIN');
    await register('dipendente', 'AnotherSecure123!', 'EMPLOYEE');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/accounts/login');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST' });
    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({ username: 'superadmin', password: 'SecurePassword123!', role: 'SUPER_ADMIN' });
    expect(fetchMock.mock.calls[1][0]).toBe('/api/accounts/register');
    expect(JSON.parse(String(fetchMock.mock.calls[1][1]?.body))).toEqual({ username: 'dipendente', password: 'AnotherSecure123!', role: 'EMPLOYEE' });
  });

  it('espone il rifiuto backend di una password non valida', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      code: 'PASSWORD_POLICY_VIOLATION',
      message: 'La password non rispetta i requisiti di sicurezza.',
      details: ['Usa almeno 12 caratteri.']
    }, 400)));

    const error = await register('utente', 'debole', 'CUSTOMER').catch((exception) => exception);

    expect(error).toBeInstanceOf(ApiRequestError);
    expect(error).toMatchObject({ status: 400, code: 'PASSWORD_POLICY_VIOLATION' });
  });

  it('rinnova la sessione autenticata senza ripetere username e ruolo', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ token: 'rotated-token', expiresAt: '2026-07-13T12:45:00Z', user: {} }));
    vi.stubGlobal('fetch', fetchMock);
    setSessionToken('current-token');

    await renewSession('SecurePassword123!');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/accounts/session/renew');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST' });
    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({ password: 'SecurePassword123!' });
    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).get('X-Session-Token')).toBe('current-token');
  });

  it('crea un prodotto con payload strutturato', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ code: 'GPU-001' }, 201));
    vi.stubGlobal('fetch', fetchMock);
    const payload = {
      code: 'GPU-001',
      name: 'Scheda video',
      description: 'Descrizione',
      category: 'HARDWARE' as const,
      brand: 'Example Brand',
      productType: 'Scheda grafica',
      usageContext: 'Gaming',
      quantity: 3,
      price: 899.9,
      discount: 5
    };

    await createProduct(payload);

    expect(fetchMock.mock.calls[0][0]).toBe('/api/products');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST', body: JSON.stringify(payload) });
  });

  it('propaga l errore di eliminazione di un prodotto referenziato', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      code: 'PRODUCT_IN_USE',
      message: 'Il prodotto e presente in ordini e non puo essere eliminato.'
    }, 409)));

    const error = await deleteProduct('GPU/001').catch((exception) => exception);

    expect(error).toBeInstanceOf(ApiRequestError);
    expect(error).toMatchObject({ status: 409, code: 'PRODUCT_IN_USE' });
  });

  it('crea e conferma un ordine usando chiavi di idempotenza distinte', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ code: 'ORD-0001', status: 'DRAFT' }, 201))
      .mockResolvedValueOnce(jsonResponse({ code: 'ORD-0001', status: 'CONFIRMED' }));
    vi.stubGlobal('fetch', fetchMock);
    const payload = {
      customer: 'Cliente Demo',
      paymentMethod: 'BANK_TRANSFER' as const,
      items: [{ productCode: 'GPU-001', quantity: 1 }]
    };

    await createOrder(payload);
    await confirmOrder('ORD/0001');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/orders');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST', body: JSON.stringify(payload) });
    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).get('Idempotency-Key')).toMatch(/^order-create-/);
    expect(fetchMock.mock.calls[1][0]).toBe('/api/orders/ORD%2F0001/confirm');
    expect(new Headers(fetchMock.mock.calls[1][1]?.headers).get('Idempotency-Key')).toMatch(/^order-confirm-ORD\/0001-/);
  });
});
