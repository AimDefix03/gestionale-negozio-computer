import { randomUUID } from 'node:crypto';
import { expect, Page, test } from '@playwright/test';

const adminUsername = process.env.E2E_USERNAME ?? process.env.GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME;
const adminPassword = process.env.E2E_PASSWORD ?? process.env.GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD;
const runId = `${Date.now()}-${randomUUID()}`;
const productCode = `E2E-${runId}`;
const productName = `Prodotto E2E ${runId}`;
const customerUsername = `e2e_customer_${runId}`;
const customerPassword = `Boreal-Cobalt-${runId}-Safe!9`;

test.describe.configure({ mode: 'serial' });

test.beforeAll(() => {
  if (!adminUsername || !adminPassword) {
    throw new Error('Imposta E2E_USERNAME ed E2E_PASSWORD oppure le credenziali bootstrap del super admin.');
  }
});

test('applica CSP e header browser senza violazioni runtime', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page);
  const response = await page.goto('/');
  const headers = response?.headers() ?? {};
  const csp = headers['content-security-policy'] ?? '';

  expect(csp).toContain("default-src 'self'");
  expect(csp).toContain("script-src 'self'");
  expect(csp).toContain("style-src 'self'");
  expect(csp).toContain("frame-ancestors 'none'");
  expect(csp).toContain("object-src 'none'");
  expect(csp).not.toContain("'unsafe-inline'");
  expect(csp).not.toContain("'unsafe-eval'");
  expect(headers['cross-origin-opener-policy']).toBe('same-origin');
  expect(headers['x-content-type-options']).toBe('nosniff');
  expect(headers['x-frame-options']).toBe('DENY');
  await expect(page.getByRole('heading', { name: 'Accedi al workspace' })).toBeVisible();
  expect(runtimeErrors).toEqual([]);
});

test('mostra un errore accessibile per credenziali non valide', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page, true);

  await page.goto('/');
  await page.getByLabel('Username').fill(`utente_inesistente_${runId}`);
  await page.getByLabel('Password').fill('Password-Non-Valida-123!');
  await page.getByLabel('Ruolo').selectOption('SUPER_ADMIN');
  await page.getByRole('button', { name: 'Accedi' }).click();

  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Accedi al workspace' })).toBeVisible();
  expect(runtimeErrors).toEqual([]);
});

test('completa catalogo, registrazione cliente e ciclo ordine', async ({ page }) => {
  const runtimeErrors = collectRuntimeErrors(page);

  await page.goto('/');
  await login(page, adminUsername!, adminPassword!, 'SUPER_ADMIN');
  await openMenuEntry(page, 'Workspace', 'Catalogo prodotti');
  await expect(page.locator('.workspace-header h1')).toHaveText('Catalogo prodotti');

  const productForm = page.locator('form.product-form');
  await productForm.getByLabel('Codice').fill(productCode);
  await productForm.getByLabel('Nome').fill(productName);
  await productForm.getByLabel('Categoria').selectOption('HARDWARE');
  await productForm.getByLabel('Brand').fill('Test Automation');
  await productForm.getByLabel('Tipo prodotto').fill('Scheda grafica');
  await productForm.getByLabel('Utilizzo opzionale').fill('Collaudo E2E');
  await productForm.getByLabel('Quantita').fill('5');
  await productForm.getByLabel('Prezzo').fill('499.90');
  await productForm.getByLabel('Sconto %').fill('0');
  await productForm.getByLabel('Descrizione').fill('Prodotto creato dallo smoke test browser sullo stack reale.');
  await productForm.getByRole('button', { name: 'Crea prodotto' }).click();

  await page.getByLabel('Cerca prodotto').fill(productCode);
  const adminProductRow = page.getByRole('row').filter({ hasText: productCode });
  await expect(adminProductRow).toContainText(productName);
  await expect(adminProductRow).toContainText('5 disponibili');

  await openMenuEntry(page, 'Operazioni', 'Report');
  await expect(page.locator('.workspace-header h1')).toHaveText('Report');
  await page.getByRole('button', { name: 'Magazzino' }).click();
  await expect(page.getByRole('heading', { name: 'Snapshot di magazzino' })).toBeVisible();
  await page.getByLabel('Cerca').fill(productCode);
  await expect(page.getByRole('row').filter({ hasText: productCode })).toContainText(productName);
  await page.setViewportSize({ width: 1024, height: 768 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'CSV' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/^report-magazzino-\d{4}-\d{2}-\d{2}\.csv$/);
  await page.setViewportSize({ width: 1440, height: 900 });

  await page.getByRole('button', { name: 'Logout' }).click();
  await registerCustomer(page, customerUsername, customerPassword);
  await openMenuEntry(page, 'Workspace', 'Catalogo prodotti');
  await page.getByLabel('Cerca prodotto').fill(productCode);

  const customerProductRow = page.getByRole('row').filter({ hasText: productCode });
  await expect(customerProductRow).toContainText(productName);
  await customerProductRow.getByRole('button', { name: 'Aggiungi' }).click();
  await expect(page.getByText(`${productName} x 1`)).toBeVisible();
  await page.getByRole('button', { name: 'Crea ordine' }).click();

  await expect(page.locator('.workspace-header h1')).toHaveText('Ordini');
  const orderRow = page.getByRole('row').filter({ hasText: customerUsername });
  await expect(orderRow).toContainText('Bozza');
  await orderRow.getByRole('button', { name: 'Conferma' }).click();
  await expect(orderRow).toContainText('Confermato');

  expect(runtimeErrors).toEqual([]);
});

async function login(page: Page, username: string, password: string, role: string) {
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByLabel('Ruolo').selectOption(role);
  await page.getByRole('button', { name: 'Accedi' }).click();
  await expect(page.locator('.workspace-header h1')).toHaveText('Dashboard operativa');
}

async function registerCustomer(page: Page, username: string, password: string) {
  await page.getByRole('button', { name: 'Registrazione' }).click();
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByLabel('Ruolo').selectOption('CUSTOMER');
  await page.getByRole('button', { name: 'Registrati' }).click();
  await expect(page.locator('.workspace-header h1')).toHaveText('Dashboard operativa');
}

async function openMenuEntry(page: Page, menu: string, entry: string) {
  await page.getByRole('button', { name: new RegExp(`^${menu}`) }).click();
  await page.getByRole('button', { name: new RegExp(`^${entry}`) }).click();
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
