import { afterEach, describe, expect, it, vi } from 'vitest';
import { changeAccountRole, changeOwnPassword, disableAccount, enableAccount, fetchAccountPage, fetchAccounts, login, register, renewSession, resetAccountPassword, revokeAccountSessions } from './accountClient';
import { fetchAuditEventPage } from './auditClient';
import { fetchCompanySettings } from './companyClient';
import { ApiRequestError, clearSessionToken, setSessionToken } from './httpClient';
import { confirmOrder, createOrder, fetchFinancialReconciliation, fetchOrderPage } from './orderClient';
import { fetchPartnerPage, linkPartnerAccount, unlinkPartnerAccount } from './partnerClient';
import { createProduct, deleteProduct } from './productClient';
import { fetchCustomerProductPage } from './productClient';
import { fetchCustomerDashboard } from './dashboardClient';
import { approvePhysicalInventory, createInitialBalance, createPhysicalInventory, fetchInventoryReconciliation, recordPhysicalInventoryCount, submitPhysicalInventory } from './inventoryClient';
import { fetchDocumentPage } from './documentClient';
import { fetchInventoryReport, fetchSalesReport } from './reportClient';
import { fetchSystemStatus } from './systemClient';

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
      .mockResolvedValueOnce(jsonResponse({ id: 2, username: 'cliente', role: 'CUSTOMER' }));
    vi.stubGlobal('fetch', fetchMock);

    await login('superadmin', 'SecurePassword123!');
    await register('cliente', 'AnotherSecure123!');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/accounts/login');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST' });
    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({ username: 'superadmin', password: 'SecurePassword123!' });
    expect(fetchMock.mock.calls[1][0]).toBe('/api/accounts/register');
    expect(JSON.parse(String(fetchMock.mock.calls[1][1]?.body))).toEqual({ username: 'cliente', password: 'AnotherSecure123!' });
  });

  it('usa comandi account espliciti senza cancellazione fisica', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ username: 'mario', enabled: false }));
    vi.stubGlobal('fetch', fetchMock);
    setSessionToken('session-token');

    await disableAccount('mario', 'Fine rapporto', 'AdminPassword123!');
    await enableAccount('mario', 'Rientro autorizzato', 'AdminPassword123!');
    await revokeAccountSessions('mario', 'Possibile compromissione', 'AdminPassword123!');
    await resetAccountPassword('mario', 'ResetPassword123!', 'Verifica completata', 'AdminPassword123!');
    await changeAccountRole('mario', 'EMPLOYEE', 'Cambio mansione', 'AdminPassword123!');
    await changeOwnPassword('CurrentPassword123!', 'NewPassword123!');

    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      '/api/accounts/mario/disable',
      '/api/accounts/mario/enable',
      '/api/accounts/mario/sessions/revoke',
      '/api/accounts/mario/password-reset',
      '/api/accounts/mario/role',
      '/api/accounts/me/password'
    ]);
    expect(fetchMock.mock.calls.slice(0, 5).every((call) => new Headers(call[1]?.headers).get('X-Reauth-Password') === 'AdminPassword123!')).toBe(true);
  });

  it('espone il rifiuto backend di una password non valida', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      code: 'PASSWORD_POLICY_VIOLATION',
      message: 'La password non rispetta i requisiti di sicurezza.',
      details: ['Usa almeno 12 caratteri.']
    }, 400)));

    const error = await register('utente', 'debole').catch((exception) => exception);

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
      price: 899.9,
      discount: 5
    };

    await createProduct(payload);

    expect(fetchMock.mock.calls[0][0]).toBe('/api/products');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST', body: JSON.stringify(payload) });
  });

  it('usa endpoint separati per catalogo e dashboard cliente', async () => {
    const controller = new AbortController();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ content: [], page: 0, size: 8, totalElements: 0, totalPages: 0, first: true, last: true }))
      .mockResolvedValueOnce(jsonResponse({ totalOrders: 0, draftOrders: 0, confirmedOrders: 0, fulfilledOrders: 0, canceledOrders: 0, recentOrders: [] }));
    vi.stubGlobal('fetch', fetchMock);

    await fetchCustomerProductPage({ page: 0, size: 8, sort: 'PRICE_ASC' }, controller.signal);
    await fetchCustomerDashboard();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/customer/catalog?page=0&size=8&sort=PRICE_ASC');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ signal: controller.signal });
    expect(fetchMock.mock.calls[1][0]).toBe('/api/customer/dashboard');
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

  it('usa un workflow approvato per le differenze di inventario', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ type: 'INITIAL_BALANCE', newQuantity: 5 }, 201))
      .mockResolvedValueOnce(jsonResponse({ id: 41, status: 'OPEN' }, 201))
      .mockResolvedValueOnce(jsonResponse({ id: 41, status: 'OPEN', countedItems: 1 }))
      .mockResolvedValueOnce(jsonResponse({ id: 41, status: 'SUBMITTED' }))
      .mockResolvedValueOnce(jsonResponse({ id: 41, status: 'APPROVED' }))
      .mockResolvedValueOnce(jsonResponse({ totalProducts: 1, balancedProducts: 1, anomalousProducts: 0, items: [] }));
    vi.stubGlobal('fetch', fetchMock);

    await createInitialBalance({ productCode: 'GPU-001', quantity: 5, reason: 'Inventario iniziale' });
    await createPhysicalInventory({ reason: 'Conteggio fisico', productCodes: ['GPU-001'] });
    await recordPhysicalInventoryCount(41, 52, { countedQuantity: 3 });
    await submitPhysicalInventory(41);
    await approvePhysicalInventory(41, { reason: 'Differenza verificata' });
    await fetchInventoryReconciliation();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/inventory/initial-balance');
    expect(fetchMock.mock.calls[1][0]).toBe('/api/inventory/counts');
    expect(fetchMock.mock.calls[2][0]).toBe('/api/inventory/counts/41/items/52');
    expect(fetchMock.mock.calls[3][0]).toBe('/api/inventory/counts/41/submit');
    expect(fetchMock.mock.calls[4][0]).toBe('/api/inventory/counts/41/approve');
    expect(fetchMock.mock.calls[5][0]).toBe('/api/inventory/reconciliation');
  });

  it('crea e conferma un ordine usando chiavi di idempotenza distinte', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ code: 'ORD-0001', status: 'DRAFT' }, 201))
      .mockResolvedValueOnce(jsonResponse({ code: 'ORD-0001', status: 'CONFIRMED' }));
    vi.stubGlobal('fetch', fetchMock);
    const payload = {
      customerType: 'WALK_IN' as const,
      walkInCustomerName: 'Cliente Demo',
      paymentMethod: 'BANK_TRANSFER' as const,
      items: [{ productCode: 'GPU-001', quantity: 1 }]
    };

    await createOrder(payload);
    await confirmOrder('ORD/0001');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/orders');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST', body: JSON.stringify(payload) });
    expect(new Headers(fetchMock.mock.calls[0][1]?.headers).get('Idempotency-Key')).toMatch(/^order-create-/);
    expect(fetchMock.mock.calls[1][0]).toBe('/api/orders/ORD%2F0001/confirm');
    expect(new Headers(fetchMock.mock.calls[1][1]?.headers).get('Idempotency-Key')).toMatch(/^order-confirm-/);
  });

  it('propaga la cancellazione alle query di ordini, clienti vendita e riconciliazione', async () => {
    const controller = new AbortController();
    const emptyPage = { content: [], page: 0, size: 8, totalElements: 0, totalPages: 0, first: true, last: true };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(emptyPage))
      .mockResolvedValueOnce(jsonResponse(emptyPage))
      .mockResolvedValueOnce(jsonResponse({ balanced: true, checkedPayments: 0, checkedReturns: 0, mismatchCount: 0, counts: {}, mismatches: [] }));
    vi.stubGlobal('fetch', fetchMock);

    await fetchOrderPage({ page: 0, size: 8 }, controller.signal);
    await fetchPartnerPage({ page: 0, size: 6, type: 'CUSTOMER' }, controller.signal);
    await fetchFinancialReconciliation(controller.signal);

    expect(fetchMock.mock.calls.map((call) => call[1]?.signal)).toEqual([
      controller.signal,
      controller.signal,
      controller.signal
    ]);
  });

  it('invia l identificatore stabile del cliente censito nella vendita assistita', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ code: 'ORD-0002', status: 'DRAFT', partnerId: 42 }, 201));
    vi.stubGlobal('fetch', fetchMock);
    const payload = {
      customerType: 'REGISTERED' as const,
      customerPartnerId: 42,
      paymentMethod: 'CARD' as const,
      items: [{ productCode: 'GPU-001', quantity: 1 }]
    };

    await createOrder(payload);

    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual(payload);
  });

  it('collega e scollega un account cliente tramite identificatore stabile', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ code: 'CLI/001', linkedAccountId: 42 }))
      .mockResolvedValueOnce(jsonResponse({ code: 'CLI/001', linkedAccountId: null }));
    vi.stubGlobal('fetch', fetchMock);

    await linkPartnerAccount('CLI/001', 42);
    await unlinkPartnerAccount('CLI/001');

    expect(fetchMock.mock.calls[0][0]).toBe('/api/partners/CLI%2F001/account-link');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'PUT', body: JSON.stringify({ accountId: 42 }) });
    expect(fetchMock.mock.calls[1][0]).toBe('/api/partners/CLI%2F001/account-link');
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'DELETE' });
  });

  it('propaga la cancellazione alle query anagrafiche e agli account collegabili', async () => {
    const controller = new AbortController();
    const emptyPage = { content: [], page: 0, size: 8, totalElements: 0, totalPages: 0, first: true, last: true };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(emptyPage))
      .mockResolvedValueOnce(jsonResponse(emptyPage));
    vi.stubGlobal('fetch', fetchMock);

    await fetchPartnerPage({ page: 0, size: 8 }, controller.signal);
    await fetchAccounts({ role: 'CUSTOMER', enabled: true, size: 8 }, controller.signal);

    expect(fetchMock.mock.calls.map((call) => call[1]?.signal)).toEqual([controller.signal, controller.signal]);
  });

  it('propaga la cancellazione alle query documenti e report', async () => {
    const controller = new AbortController();
    const emptyPage = { content: [], page: 0, size: 8, totalElements: 0, totalPages: 0, first: true, last: true };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(emptyPage))
      .mockResolvedValueOnce(jsonResponse({ orders: [], topProducts: [] }))
      .mockResolvedValueOnce(jsonResponse({ products: [] }));
    vi.stubGlobal('fetch', fetchMock);

    await fetchDocumentPage({ page: 0, size: 8 }, controller.signal);
    await fetchSalesReport({ status: 'FULFILLED' }, controller.signal);
    await fetchInventoryReport({ stock: 'LOW' }, controller.signal);

    expect(fetchMock.mock.calls.map((call) => call[1]?.signal)).toEqual([
      controller.signal,
      controller.signal,
      controller.signal
    ]);
  });

  it('propaga la cancellazione alle query amministrative', async () => {
    const controller = new AbortController();
    const emptyPage = { content: [], page: 0, size: 8, totalElements: 0, totalPages: 0, first: true, last: true };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(emptyPage))
      .mockResolvedValueOnce(jsonResponse(emptyPage))
      .mockResolvedValueOnce(jsonResponse({ configured: false }))
      .mockResolvedValueOnce(jsonResponse({ status: 'UP' }));
    vi.stubGlobal('fetch', fetchMock);

    await fetchAccountPage({ page: 0, size: 8 }, controller.signal);
    await fetchAuditEventPage({ page: 0, size: 8 }, controller.signal);
    await fetchCompanySettings(controller.signal);
    await fetchSystemStatus(controller.signal);

    expect(fetchMock.mock.calls.map((call) => call[1]?.signal)).toEqual([
      controller.signal,
      controller.signal,
      controller.signal,
      controller.signal
    ]);
  });
});
