import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, it, vi } from 'vitest';
import type { AuditEvent, CompanySettings, PageResponse, SystemStatus, UserAccount } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import AdministrationExperience from './AdministrationExperience';
import useAdministrationFlow from './useAdministrationFlow';

const apiMocks = vi.hoisted(() => ({
  changeAccountRole: vi.fn(),
  createAccount: vi.fn(),
  disableAccount: vi.fn(),
  enableAccount: vi.fn(),
  fetchAccountPage: vi.fn(),
  fetchAuditEventPage: vi.fn(),
  fetchCompanySettings: vi.fn(),
  fetchSystemStatus: vi.fn(),
  resetAccountPassword: vi.fn(),
  revokeAccountSessions: vi.fn(),
  updateCompanySettings: vi.fn()
}));

vi.mock('../../api', async () => ({
  ...await vi.importActual<typeof import('../../api')>('../../api'),
  ...apiMocks
}));

const superAdmin = accountFixture('superadmin', 'SUPER_ADMIN');
const employee = accountFixture('mario', 'EMPLOYEE');
const auditEvent = auditFixture();
const companySettings = companyFixture();
const systemStatus = systemFixture();

const executeCommand: CommandExecutor = async <T,>(options: CommandExecutionOptions<T>) => {
  const value = await options.command();
  options.applyResponse(value);
  options.afterConfirmed?.(value);
  return { status: 'saved', value };
};

beforeEach(() => {
  vi.clearAllMocks();
  apiMocks.fetchAccountPage.mockResolvedValue(pageOf([superAdmin]));
  apiMocks.fetchAuditEventPage.mockResolvedValue(pageOf([auditEvent]));
  apiMocks.fetchCompanySettings.mockResolvedValue(companySettings);
  apiMocks.fetchSystemStatus.mockResolvedValue(systemStatus);
  apiMocks.createAccount.mockResolvedValue(employee);
});

it('compone la gestione account e applica la creazione confermata', async () => {
  const user = userEvent.setup();
  render(<AdministrationHarness view="accounts" />);

  await waitFor(() => expect(screen.getByText('superadmin')).toBeInTheDocument());
  await user.type(screen.getByLabelText('Username'), 'mario');
  await user.type(screen.getByLabelText('Password iniziale'), 'Password-Sicura-123!');
  await user.type(screen.getByLabelText('Password operatore'), 'Password-Operatore-123!');
  await user.click(screen.getByRole('button', { name: 'Crea account' }));

  await waitFor(() => expect(screen.getByText('mario')).toBeInTheDocument());
  expect(apiMocks.createAccount).toHaveBeenCalledWith('mario', 'Password-Sicura-123!', 'EMPLOYEE', 'Password-Operatore-123!');
  expect(screen.getByLabelText('Username')).toHaveValue('');
});

it('mostra l errore account iniziale e rende disponibile il retry', async () => {
  const user = userEvent.setup();
  apiMocks.fetchAccountPage.mockRejectedValue(new Error('Servizio account non disponibile'));

  render(<AdministrationHarness view="accounts" />);

  expect(await screen.findByRole('heading', { name: 'Account non disponibili' })).toBeInTheDocument();
  expect(screen.getByText('Servizio account non disponibile')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Riprova' }));
  await waitFor(() => expect(apiMocks.fetchAccountPage).toHaveBeenCalledTimes(2));
});

it('renderizza l audit log senza stato amministrativo in App', async () => {
  render(<AdministrationHarness view="audit" />);

  expect(await screen.findByText('ACCOUNT_CREATED')).toBeInTheDocument();
  expect(screen.getByText(/req-1/)).toBeInTheDocument();
});

it('attiva il monitoraggio quando viene aperta la relativa vista', async () => {
  render(<AdministrationHarness view="monitoring" />);

  expect(await screen.findByRole('heading', { name: 'Stato sistema' })).toBeInTheDocument();
  expect(screen.getByText('gestionale-api · 1 min')).toBeInTheDocument();
  expect(apiMocks.fetchSystemStatus).toHaveBeenCalledOnce();
});

function AdministrationHarness({ view }: { view: 'accounts' | 'audit' | 'company' | 'monitoring' }) {
  const flow = useAdministrationFlow({
    enabled: true,
    currentUser: superAdmin,
    canManageAccounts: true,
    canManageCompanySettings: true,
    canViewAudit: true,
    monitoringActive: view === 'monitoring',
    executeCommand,
    onAccountApplied: vi.fn(),
    onRefreshCustomerAccounts: vi.fn().mockResolvedValue(undefined),
    onNotice: vi.fn()
  });
  return <AdministrationExperience view={view} flow={flow} isSuperAdmin busy={false} />;
}

function pageOf<T>(content: T[]): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 8,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true
  };
}

function accountFixture(username: string, role: UserAccount['role']): UserAccount {
  return {
    id: username.length,
    username,
    role,
    roleLabel: role === 'SUPER_ADMIN' ? 'Super admin' : role === 'EMPLOYEE' ? 'Dipendente' : role,
    permissions: role === 'SUPER_ADMIN' ? ['MANAGE_ACCOUNTS', 'MANAGE_COMPANY_SETTINGS', 'VIEW_AUDIT'] : [],
    enabled: true,
    disabledAt: null,
    disabledBy: null,
    disabledReason: null
  };
}

function auditFixture(): AuditEvent {
  return {
    id: 1,
    timestamp: '2026-08-18T10:00:00Z',
    actor: 'superadmin',
    role: 'SUPER_ADMIN',
    action: 'ACCOUNT_CREATED',
    target: 'mario',
    details: 'Account creato',
    requestId: 'req-1',
    source: 'API',
    entityType: 'USER_ACCOUNT',
    category: 'ACCOUNT',
    severity: 'INFO'
  };
}

function companyFixture(): CompanySettings {
  return {
    version: 1,
    configured: true,
    missingDocumentFields: [],
    legalName: 'Negozio Test',
    taxCode: 'TSTNGZ80A01F839A',
    vatNumber: '01234567890',
    email: 'negozio@example.test',
    phone: '',
    address: 'Via Test 1',
    postalCode: '80100',
    city: 'Napoli',
    province: 'NA',
    countryCode: 'IT',
    timeZone: 'Europe/Rome',
    defaultVatRate: 0.22,
    invoicePrefix: 'FS',
    creditNotePrefix: 'NC',
    numberPadding: 4,
    updatedAt: '2026-08-18T10:00:00Z',
    updatedBy: 'superadmin'
  };
}

function systemFixture(): SystemStatus {
  return {
    status: 'UP',
    timestamp: '2026-08-18T10:00:00Z',
    application: 'gestionale-api',
    activeProfile: 'test',
    uptimeMs: 60_000,
    database: { status: 'UP', latencyMs: 3, name: 'PostgreSQL', message: 'Disponibile' },
    runtime: { usedMemoryBytes: 1024, maxMemoryBytes: 4096, availableProcessors: 4 },
    security: { activeSessions: 2, lockedLoginAttempts: 0, recentLoginAttempts: 1 },
    audit: { criticalLast24h: 0, warningsLast24h: 1, recentImportantEvents: [auditEvent] },
    recentErrors: []
  };
}
