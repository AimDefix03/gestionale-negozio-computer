import { randomUUID } from 'node:crypto';
import { APIResponse, expect, Page, Response, test } from '@playwright/test';

type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'EMPLOYEE' | 'CUSTOMER';

type AuthSession = {
  token: string;
  expiresAt: string;
  user: {
    username: string;
    role: UserRole;
  };
};

type Product = {
  code: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
};

type PhysicalInventorySession = {
  id: number;
  code: string;
  status: 'OPEN' | 'SUBMITTED' | 'APPROVED' | 'CANCELED';
  items: Array<{
    id: number;
    productCode: string;
    differenceQuantity: number | null;
  }>;
};

type PaymentTransaction = {
  type: 'RECEIPT' | 'REFUND' | 'REVERSAL' | 'RECONCILIATION';
  amount: number;
  returnCode: string | null;
};

type OrderReturn = {
  code: string;
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'RECEIVED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';
  totalAmount: number;
  refundTransactions: PaymentTransaction[];
};

type Order = {
  code: string;
  total: number;
  status: 'DRAFT' | 'CONFIRMED' | 'FULFILLED' | 'CANCELED';
  payment: {
    requestedAmount: number;
    paidAmount: number | null;
    refundedAmount: number | null;
    netPaidAmount: number | null;
    outstandingAmount: number | null;
    transactions: PaymentTransaction[];
  };
  returns: OrderReturn[];
};

type PageResponse<T> = {
  content: T[];
};

const adminUsername = process.env.E2E_USERNAME ?? process.env.GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME;
const adminPassword = process.env.E2E_PASSWORD ?? process.env.GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD;
const suffix = `${Date.now()}${randomUUID().replace(/-/g, '').slice(0, 8)}`;
const employeeUsername = `e2e_employee_${suffix}`;
const employeePassword = 'Quasar!Ledger-Employee-9274';
const customerUsername = `e2e_customer_gate_${suffix}`;
const customerPassword = 'Orbit!Customer-5482-Strong';
const partnerCode = `CLI-${suffix}`;
const partnerName = `Cliente E2E ${suffix}`;
const firstProductCode = `E2E-A-${suffix}`;
const firstProductName = `Componente E2E A ${suffix}`;
const secondProductCode = `E2E-B-${suffix}`;
const secondProductName = `Componente E2E B ${suffix}`;

let adminToken = '';
let employeeToken = '';
let fulfilledOrder: Order;
let canceledOrder: Order;

test.describe.configure({ mode: 'serial' });

test.beforeAll(() => {
  if (!adminUsername || !adminPassword) {
    throw new Error('Imposta E2E_USERNAME ed E2E_PASSWORD oppure le credenziali bootstrap del super admin.');
  }
});

test('super admin crea dipendente', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page);
  await page.goto('/');
  const adminSession = await login(page, adminUsername!, adminPassword!);
  adminToken = adminSession.token;
  expect(adminSession.user.role).toBe('SUPER_ADMIN');

  await openMenuEntry(page, 'Amministrazione', 'Account');
  const provisioningForm = page.locator('form').filter({ has: page.getByRole('button', { name: 'Crea account' }) });
  await provisioningForm.getByLabel('Username').fill(employeeUsername);
  await provisioningForm.getByLabel('Password iniziale').fill(employeePassword);
  await provisioningForm.getByLabel('Ruolo').selectOption('EMPLOYEE');
  await provisioningForm.getByLabel('Password operatore').fill(adminPassword!);

  const responsePromise = waitForApiResponse(page, 'POST', /^\/api\/accounts$/);
  await provisioningForm.getByRole('button', { name: 'Crea account' }).click();
  const employee = await readJson<{ username: string; role: UserRole }>(await responsePromise);

  expect(employee).toMatchObject({ username: employeeUsername, role: 'EMPLOYEE' });
  await expect(page.getByRole('status').filter({ hasText: `Account ${employeeUsername} creato.` })).toBeVisible();
  expect(runtimeErrors).toEqual([]);
});

test('dipendente crea cliente e vendita, poi completa carico inventario fisico riserva ed evasione', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page);
  await page.goto('/');
  const session = await login(page, employeeUsername, employeePassword);
  employeeToken = session.token;
  expect(session.user.role).toBe('EMPLOYEE');

  await createPartner(page);
  await createProduct(page, firstProductCode, firstProductName, '199.90');
  await createProduct(page, secondProductCode, secondProductName, '249.90');

  await openMenuEntry(page, 'Operazioni', 'Magazzino');
  await recordInventoryMovement(page, firstProductCode, 'INITIAL_BALANCE', '1', 'Saldo iniziale gate MVP');
  await recordInventoryMovement(page, firstProductCode, 'LOAD', '8', 'Carico fornitura gate MVP');
  await recordInventoryMovement(page, secondProductCode, 'INITIAL_BALANCE', '6', 'Saldo iniziale seconda riga gate MVP');
  await applyPhysicalInventoryCount(page, firstProductCode, 10);

  const preparedProduct = await apiGet<Product>(page, employeeToken, `/api/products/${encodeURIComponent(firstProductCode)}`);
  expect(preparedProduct).toMatchObject({ quantity: 10, reservedQuantity: 0, availableQuantity: 10 });

  fulfilledOrder = await createAssistedOrder(page, 2, true);
  const orderRow = page.getByRole('row').filter({ hasText: fulfilledOrder.code });

  const confirmationPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${fulfilledOrder.code}/confirm$`));
  await orderRow.getByRole('button', { name: 'Conferma' }).click();
  const confirmedOrder = await readJson<Order>(await confirmationPromise);
  expect(confirmedOrder.status).toBe('CONFIRMED');

  const reservedProduct = await apiGet<Product>(page, employeeToken, `/api/products/${encodeURIComponent(firstProductCode)}`);
  expect(reservedProduct).toMatchObject({ quantity: 10, reservedQuantity: 2, availableQuantity: 8 });

  const fulfillmentPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${fulfilledOrder.code}/fulfill$`));
  await orderRow.getByRole('button', { name: 'Evadi' }).click();
  fulfilledOrder = await readJson<Order>(await fulfillmentPromise);
  expect(fulfilledOrder.status).toBe('FULFILLED');

  const fulfilledFirstProduct = await apiGet<Product>(page, employeeToken, `/api/products/${encodeURIComponent(firstProductCode)}`);
  const fulfilledSecondProduct = await apiGet<Product>(page, employeeToken, `/api/products/${encodeURIComponent(secondProductCode)}`);
  expect(fulfilledFirstProduct).toMatchObject({ quantity: 8, reservedQuantity: 0, availableQuantity: 8 });
  expect(fulfilledSecondProduct).toMatchObject({ quantity: 5, reservedQuantity: 0, availableQuantity: 5 });
  expect(runtimeErrors).toEqual([]);
});

test('acconto, annullo e storno restano atomici', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page);
  await page.goto('/');
  const session = await login(page, employeeUsername, employeePassword);
  const order = await createAssistedOrder(page, 1, false);
  const orderRow = page.getByRole('row').filter({ hasText: order.code });

  const confirmationPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${order.code}/confirm$`));
  await orderRow.getByRole('button', { name: 'Conferma' }).click();
  expect((await readJson<Order>(await confirmationPromise)).status).toBe('CONFIRMED');
  await openOrderOperations(page, order.code);

  const receiptForm = page.locator('form').filter({ has: page.getByRole('button', { name: 'Registra incasso' }) });
  await receiptForm.getByLabel('Importo').fill('25');
  await receiptForm.getByLabel('Riferimento').fill(`POS-${suffix}`);
  await receiptForm.getByLabel('Causale').fill('Acconto gate MVP');
  const receiptPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${order.code}/payments/receipts$`));
  await receiptForm.getByRole('button', { name: 'Registra incasso' }).click();
  const paidOrder = await readJson<Order>(await receiptPromise);
  expect(paidOrder.payment.transactions).toEqual(expect.arrayContaining([expect.objectContaining({ type: 'RECEIPT', amount: 25 })]));

  const cancellationForm = page.locator('form.cancellation-operation');
  await cancellationForm.getByLabel('Riferimento storno').fill(`STORNO-${suffix}`);
  await cancellationForm.getByLabel('Motivazione').fill('Annullamento controllato gate MVP');
  const cancellationPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${order.code}/cancel$`));
  await cancellationForm.getByRole('button', { name: 'Annulla e registra storno' }).click();
  canceledOrder = await readJson<Order>(await cancellationPromise);

  expect(canceledOrder.status).toBe('CANCELED');
  expect(canceledOrder.payment.netPaidAmount).toBe(0);
  expect(canceledOrder.payment.transactions).toEqual(expect.arrayContaining([
    expect.objectContaining({ type: 'RECEIPT', amount: 25 }),
    expect.objectContaining({ type: 'REVERSAL', amount: 25 })
  ]));
  const product = await apiGet<Product>(page, session.token, `/api/products/${encodeURIComponent(firstProductCode)}`);
  expect(product.reservedQuantity).toBe(0);
  expect(runtimeErrors).toEqual([]);
});

test('reso multi-riga, ricezione e rimborso aggiornano ledger e giacenze', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page);
  await page.goto('/');
  const session = await login(page, employeeUsername, employeePassword);
  await openOrderOperations(page, fulfilledOrder.code);

  const receiptForm = page.locator('form').filter({ has: page.getByRole('button', { name: 'Registra incasso' }) });
  await receiptForm.getByLabel('Importo').fill(String(fulfilledOrder.total));
  await receiptForm.getByLabel('Riferimento').fill(`SALDO-${suffix}`);
  await receiptForm.getByLabel('Causale').fill('Saldo per reso gate MVP');
  const receiptPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${fulfilledOrder.code}/payments/receipts$`));
  await receiptForm.getByRole('button', { name: 'Registra incasso' }).click();
  const paidOrder = await readJson<Order>(await receiptPromise);
  expect(paidOrder.payment.outstandingAmount).toBe(0);

  const returnForm = page.locator('form').filter({ has: page.getByRole('button', { name: 'Richiedi reso' }) });
  await returnForm.getByLabel(`Quantita da restituire per ${firstProductCode}`).fill('1');
  await returnForm.getByLabel(`Quantita da restituire per ${secondProductCode}`).fill('1');
  await returnForm.getByLabel('Motivazione').fill('Reso multi-riga gate MVP');
  const returnRequestPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${fulfilledOrder.code}/returns$`));
  await returnForm.getByRole('button', { name: 'Richiedi reso' }).click();
  const requestedOrder = await readJson<Order>(await returnRequestPromise);
  const requestedReturn = requestedOrder.returns.find((orderReturn) => orderReturn.status === 'REQUESTED');
  expect(requestedReturn).toBeDefined();

  const returnCard = page.locator('.return-card').filter({ hasText: requestedReturn!.code });
  const approvalPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${fulfilledOrder.code}/returns/${requestedReturn!.code}/approve$`));
  await returnCard.getByRole('button', { name: 'Approva' }).click();
  const approvedOrder = await readJson<Order>(await approvalPromise);
  expect(approvedOrder.returns.find((item) => item.code === requestedReturn!.code)?.status).toBe('APPROVED');

  const receiptReturnPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${fulfilledOrder.code}/returns/${requestedReturn!.code}/receive$`));
  await returnCard.getByRole('button', { name: 'Registra ricezione' }).click();
  const receivedOrder = await readJson<Order>(await receiptReturnPromise);
  expect(receivedOrder.returns.find((item) => item.code === requestedReturn!.code)?.status).toBe('RECEIVED');

  const returnedFirstProduct = await apiGet<Product>(page, session.token, `/api/products/${encodeURIComponent(firstProductCode)}`);
  const returnedSecondProduct = await apiGet<Product>(page, session.token, `/api/products/${encodeURIComponent(secondProductCode)}`);
  expect(returnedFirstProduct.quantity).toBe(9);
  expect(returnedSecondProduct.quantity).toBe(6);

  await returnCard.getByRole('button', { name: 'Prepara rimborso' }).click();
  const refundForm = page.locator('form.refund-form');
  await refundForm.getByLabel('Riferimento').fill(`RIMBORSO-${suffix}`);
  await refundForm.getByLabel('Causale').fill('Rimborso reso multi-riga gate MVP');
  const refundPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/orders/${fulfilledOrder.code}/returns/${requestedReturn!.code}/refund$`));
  await refundForm.getByRole('button', { name: 'Registra rimborso' }).click();
  const refundedOrder = await readJson<Order>(await refundPromise);
  const refundedReturn = refundedOrder.returns.find((item) => item.code === requestedReturn!.code);

  expect(refundedReturn?.status).toBe('REFUNDED');
  expect(refundedReturn?.refundTransactions).toEqual(expect.arrayContaining([
    expect.objectContaining({ type: 'REFUND', returnCode: requestedReturn!.code })
  ]));
  expect(runtimeErrors).toEqual([]);
});

test('sessione scaduta durante query consente la ri-autenticazione senza perdere la schermata', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page, true);
  await page.goto('/');
  await login(page, employeeUsername, employeePassword);
  await openMenuEntry(page, 'Workspace', 'Anagrafiche');

  const requestId = `e2e-session-${suffix}`;
  let intercepted = false;
  await page.route('**/api/partners?**', async (route) => {
    const url = new URL(route.request().url());
    if (!intercepted && url.searchParams.get('q') === requestId) {
      intercepted = true;
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        headers: { 'X-Request-Id': requestId },
        body: JSON.stringify({
          status: 401,
          code: 'AUTH_UNAUTHORIZED',
          message: 'Sessione scaduta durante la query.',
          path: '/api/partners',
          requestId
        })
      });
      return;
    }
    await route.fallback();
  });

  await page.getByLabel('Cerca', { exact: true }).fill(requestId);
  const dialog = page.getByRole('dialog', { name: 'Riconferma accesso' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole('alert')).toContainText(requestId);
  await dialog.getByLabel('Password').fill(employeePassword);
  await dialog.getByRole('button', { name: 'Rinnova sessione' }).click();

  await expect(dialog).toBeHidden();
  await expect(page.getByRole('status').filter({ hasText: 'Sessione rinnovata. Puoi continuare.' })).toBeVisible();
  await expect(page.locator('.workspace-header h1')).toHaveText('Anagrafiche');
  await page.getByRole('button', { name: 'Riprova' }).click();
  await expect(page.getByRole('heading', { name: 'Aggiornamento non riuscito' })).toBeHidden();
  await expect(page.getByRole('table', { name: 'Anagrafiche' })).toBeVisible();
  expect(intercepted).toBe(true);
  expect(runtimeErrors).toEqual([]);
});

test('autorizzazione cliente negativa impedisce accesso a dati e ordini altrui', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page, true);
  await page.goto('/');
  const customerSession = await registerCustomer(page, customerUsername, customerPassword);
  expect(customerSession.user.role).toBe('CUSTOMER');

  const mainMenu = page.getByRole('region', { name: 'Menu principale' });
  await mainMenu.getByRole('button', { name: /^Workspace/ }).click();
  await expect(mainMenu.getByRole('button', { name: /^Anagrafiche/ })).toHaveCount(0);
  await page.keyboard.press('Escape');

  const productResponse = await page.request.get('/api/products?page=0&size=1', {
    headers: sessionHeaders(customerSession.token)
  });
  expect(productResponse.status()).toBe(403);

  const foreignOrderResponse = await page.request.get(`/api/orders/${encodeURIComponent(fulfilledOrder.code)}`, {
    headers: sessionHeaders(customerSession.token)
  });
  expect(foreignOrderResponse.status()).toBe(403);
  expect((await foreignOrderResponse.json() as { message: string }).message).toContain('Puoi operare solo sui tuoi ordini.');

  const ownOrders = await apiGet<PageResponse<Order>>(page, customerSession.token, '/api/orders?page=0&size=25');
  expect(ownOrders.content.map((order) => order.code)).not.toContain(fulfilledOrder.code);
  expect(ownOrders.content.map((order) => order.code)).not.toContain(canceledOrder.code);
  expect(runtimeErrors).toEqual([]);
});

async function createPartner(page: Page) {
  await openMenuEntry(page, 'Workspace', 'Anagrafiche');
  const form = page.locator('form').filter({ has: page.getByRole('button', { name: 'Crea anagrafica' }) });
  await form.getByLabel('Codice', { exact: true }).fill(partnerCode);
  await form.getByLabel('Tipo').selectOption('CUSTOMER');
  await form.getByLabel('Nome / Ragione sociale').fill(partnerName);
  await form.getByLabel('Email').fill(`cliente-${suffix}@example.test`);
  await form.getByLabel('Citta').fill('Napoli');
  const responsePromise = waitForApiResponse(page, 'POST', /^\/api\/partners$/);
  await form.getByRole('button', { name: 'Crea anagrafica' }).click();
  const partner = await readJson<{ code: string; displayName: string }>(await responsePromise);
  expect(partner).toMatchObject({ code: partnerCode, displayName: partnerName });
}

async function createProduct(page: Page, code: string, name: string, price: string) {
  await openMenuEntry(page, 'Workspace', 'Catalogo prodotti');
  const form = page.locator('form.product-form');
  await form.getByLabel('Codice', { exact: true }).fill(code);
  await form.getByLabel('Nome').fill(name);
  await form.getByLabel('Categoria').selectOption('HARDWARE');
  await form.getByLabel('Brand').fill('E2E Components');
  await form.getByLabel('Tipo prodotto').fill('Componente di test');
  await form.getByLabel('Utilizzo opzionale').fill('Gate MVP');
  await form.getByLabel('Prezzo').fill(price);
  await form.getByLabel('Sconto %').fill('0');
  await form.getByLabel('Descrizione').fill('Prodotto effimero creato dal gate E2E dell MVP.');
  const responsePromise = waitForApiResponse(page, 'POST', /^\/api\/products$/);
  await form.getByRole('button', { name: 'Crea prodotto' }).click();
  const product = await readJson<Product>(await responsePromise);
  expect(product.code).toBe(code);
  await expect(page.getByRole('status').filter({ hasText: `Prodotto ${code} creato.` })).toBeVisible();
  await expect(form.getByLabel('Codice', { exact: true })).toHaveValue('');
}

async function recordInventoryMovement(page: Page, code: string, operation: 'INITIAL_BALANCE' | 'LOAD' | 'UNLOAD', quantity: string, reason: string) {
  const form = page.locator('form').filter({ has: page.getByRole('button', { name: 'Registra nel ledger' }) });
  await form.getByLabel('Prodotto').selectOption(code);
  await form.getByLabel('Operazione').selectOption(operation);
  const quantityLabel = operation === 'INITIAL_BALANCE' ? 'Giacenza iniziale' : 'Quantita';
  await form.getByLabel(quantityLabel).fill(quantity);
  await form.getByLabel('Causale').fill(reason);
  const path = operation === 'INITIAL_BALANCE'
    ? /^\/api\/inventory\/initial-balance$/
    : /^\/api\/inventory\/movements$/;
  const responsePromise = waitForApiResponse(page, 'POST', path);
  await form.getByRole('button', { name: 'Registra nel ledger' }).click();
  await readJson(await responsePromise);
}

async function applyPhysicalInventoryCount(page: Page, code: string, countedQuantity: number) {
  await page.getByRole('button', { name: 'Nuovo inventario' }).click();
  const createForm = page.locator('form.physical-inventory-create');
  await createForm.getByLabel('Motivo del conteggio').fill('Verifica fisica gate MVP');
  await createForm.getByLabel('Prodotti').selectOption(code);

  const createPromise = waitForApiResponse(page, 'POST', /^\/api\/inventory\/counts$/);
  await createForm.getByRole('button', { name: 'Apri sessione' }).click();
  const session = await readJson<PhysicalInventorySession>(await createPromise);
  const item = session.items.find((candidate) => candidate.productCode === code);
  expect(item).toBeDefined();

  const itemCard = page.locator('.physical-inventory-item').filter({ hasText: code });
  await expect(itemCard).toBeVisible();
  await itemCard.getByLabel('Nota').fill('Conteggio fisico eseguito dal dipendente E2E');
  const countedQuantityInput = itemCard.getByLabel(/Quantit. contata/);
  await countedQuantityInput.fill(String(countedQuantity));
  await expect(countedQuantityInput).toHaveValue(String(countedQuantity));
  const countPromise = waitForApiResponse(page, 'PUT', new RegExp(`^/api/inventory/counts/${session.id}/items/${item!.id}$`));
  await itemCard.getByRole('button', { name: 'Registra' }).click();
  const counted = await readJson<PhysicalInventorySession>(await countPromise);
  expect(counted.items.find((candidate) => candidate.id === item!.id)?.differenceQuantity).toBe(1);

  const submitPromise = waitForApiResponse(page, 'POST', new RegExp(`^/api/inventory/counts/${session.id}/submit$`));
  await page.getByRole('button', { name: 'Invia per approvazione' }).click();
  expect((await readJson<PhysicalInventorySession>(await submitPromise)).status).toBe('SUBMITTED');

  const approvalResponse = await page.request.post(`/api/inventory/counts/${session.id}/approve`, {
    headers: {
      ...sessionHeaders(adminToken),
      'Idempotency-Key': `physical-inventory-approve-${randomUUID()}`
    },
    data: { reason: 'Differenza verificata dal super admin E2E' }
  });
  expect((await readJson<PhysicalInventorySession>(approvalResponse)).status).toBe('APPROVED');
}

async function createAssistedOrder(page: Page, firstProductQuantity: number, includeSecondProduct: boolean): Promise<Order> {
  await openMenuEntry(page, 'Operazioni', 'Vendita');
  await page.getByLabel('Cerca cliente').fill(partnerCode);
  await page.getByRole('button', { name: new RegExp(partnerName) }).click();

  await page.getByLabel('Cerca prodotto').fill(firstProductCode);
  const firstProductRow = page.getByRole('row').filter({ hasText: firstProductCode });
  await firstProductRow.getByRole('button', { name: 'Aggiungi' }).click();
  const firstCartLine = page.getByRole('listitem').filter({ hasText: firstProductName });
  for (let quantity = 1; quantity < firstProductQuantity; quantity += 1) {
    await firstCartLine.getByRole('button', { name: `Aumenta quantita ${firstProductName}` }).click();
  }

  if (includeSecondProduct) {
    await page.getByLabel('Cerca prodotto').fill(secondProductCode);
    const secondProductRow = page.getByRole('row').filter({ hasText: secondProductCode });
    await secondProductRow.getByRole('button', { name: 'Aggiungi' }).click();
  }

  const responsePromise = waitForApiResponse(page, 'POST', /^\/api\/orders$/);
  await page.getByRole('button', { name: 'Crea bozza ordine' }).click();
  const order = await readJson<Order>(await responsePromise);
  await expect(page.locator('.workspace-header h1')).toHaveText('Ordini cliente');
  await expect(page.getByRole('row').filter({ hasText: order.code })).toBeVisible();
  return order;
}

async function openOrderOperations(page: Page, orderCode: string) {
  await openMenuEntry(page, 'Operazioni', 'Ordini');
  await page.getByLabel('Cerca', { exact: true }).fill(orderCode);
  const row = page.getByRole('row').filter({ hasText: orderCode });
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: 'Operazioni' }).click();
  await expect(page.locator('.order-operations').getByRole('heading', { name: orderCode })).toBeVisible();
}

async function login(page: Page, username: string, password: string): Promise<AuthSession> {
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  const responsePromise = waitForApiResponse(page, 'POST', /^\/api\/accounts\/login$/);
  await page.getByRole('button', { name: 'Accedi' }).click();
  const session = await readJson<AuthSession>(await responsePromise);
  await expect(page.getByRole('heading', { name: 'Dashboard operativa' })).toBeVisible();
  return session;
}

async function registerCustomer(page: Page, username: string, password: string): Promise<AuthSession> {
  await page.getByRole('button', { name: 'Registrazione' }).click();
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  const registrationPromise = waitForApiResponse(page, 'POST', /^\/api\/accounts\/register$/);
  const loginPromise = waitForApiResponse(page, 'POST', /^\/api\/accounts\/login$/);
  await page.getByRole('button', { name: 'Registrati' }).click();
  await readJson(await registrationPromise);
  const session = await readJson<AuthSession>(await loginPromise);
  await expect(page.getByRole('heading', { name: 'Dashboard operativa' })).toBeVisible();
  return session;
}

async function apiGet<T>(page: Page, token: string, path: string): Promise<T> {
  const response = await page.request.get(path, { headers: sessionHeaders(token) });
  return readJson<T>(response);
}

function waitForApiResponse(page: Page, method: string, path: RegExp): Promise<Response> {
  return page.waitForResponse((response) => response.request().method() === method && path.test(new URL(response.url()).pathname));
}

async function readJson<T = unknown>(response: Response | APIResponse): Promise<T> {
  const body = await response.text();
  expect(response.ok(), `${response.url()} -> ${response.status()} ${body}`).toBe(true);
  return body ? JSON.parse(body) as T : undefined as T;
}

function sessionHeaders(token: string) {
  return {
    'Content-Type': 'application/json',
    'X-Session-Token': token,
    'X-Request-Id': `e2e-${randomUUID()}`
  };
}

async function openMenuEntry(page: Page, menu: string, entry: string) {
  const mainMenu = page.getByRole('region', { name: 'Menu principale' });
  await mainMenu.getByRole('button', { name: new RegExp(`^${menu}`) }).click();
  await mainMenu.getByRole('button', { name: new RegExp(`^${entry}`) }).click();
}

function collectRuntimeErrors(page: Page, allowExpectedHttpFailure = false) {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    const text = message.text();
    if (message.type() === 'error' && (!allowExpectedHttpFailure || !text.startsWith('Failed to load resource:'))) {
      errors.push(text);
    }
  });
  return errors;
}
