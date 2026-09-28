import AxeBuilder from '@axe-core/playwright';
import { expect, Page, test } from '@playwright/test';

const adminUsername = process.env.E2E_USERNAME ?? process.env.GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME;
const adminPassword = process.env.E2E_PASSWORD ?? process.env.GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD;

test('la pagina di accesso non presenta violazioni axe critiche o serie', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Accedi al workspace' })).toBeVisible();
  await expectNoSeriousAxeViolations(page);
  await expect(page.getByRole('main')).toBeVisible();
  await expect(page.getByLabel('Username')).toBeVisible();
  await expect(page.getByLabel('Password')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Accedi' })).toBeVisible();
  await expectNoHorizontalOverflow(page);
});

test('il flusso principale e le bozze sono utilizzabili da tastiera', async ({ page }) => {
  test.skip(!adminUsername || !adminPassword, 'Credenziali E2E non configurate.');
  await login(page, adminUsername!, adminPassword!);

  const mainMenu = page.getByRole('region', { name: 'Menu principale' });
  const workspaceTrigger = mainMenu.getByRole('button', { name: /^Workspace/ });
  await workspaceTrigger.focus();
  await page.keyboard.press('Enter');
  const catalogEntry = mainMenu.getByRole('button', { name: /^Catalogo prodotti/ });
  await catalogEntry.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Catalogo prodotti', exact: true }).first()).toBeVisible();

  await page.getByLabel('Codice').fill('BOZZA-A11Y');
  const operationsTrigger = mainMenu.getByRole('button', { name: /^Operazioni/ });
  await operationsTrigger.focus();
  await page.keyboard.press('Enter');
  const inventoryEntry = mainMenu.getByRole('button', { name: /^Magazzino/ });
  await inventoryEntry.focus();
  await page.keyboard.press('Enter');

  const dialog = page.getByRole('dialog', { name: /Aprire Magazzino/ });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('Nuovo prodotto')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
  await expect(inventoryEntry).toBeFocused();

  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Apri scheda' }).click();
  await expect(page.getByRole('heading', { name: 'Magazzino', exact: true }).first()).toBeVisible();

  const inventoryTab = page.locator('#workspace-tab-inventory');
  await inventoryTab.focus();
  await page.keyboard.press('ArrowLeft');
  await expect(page.locator('#workspace-tab-catalog')).toHaveAttribute('aria-current', 'page');
  await expectNoSeriousAxeViolations(page);
  await expectNoHorizontalOverflow(page);
});

async function login(page: Page, username: string, password: string) {
  await page.goto('/');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Accedi' }).click();
  await expect(page.getByRole('heading', { name: 'Dashboard operativa' })).toBeVisible();
}

async function expectNoSeriousAxeViolations(page: Page) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();
  const violations = results.violations.filter((violation) => violation.impact === 'critical' || violation.impact === 'serious');
  expect(violations, violations.map((violation) => `${violation.id}: ${violation.help}`).join('\n')).toEqual([]);
}

async function expectNoHorizontalOverflow(page: Page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
}
