import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type {
  AuditEvent,
  CompanySettings,
  PageResponse,
  SystemStatus,
  UserAccount
} from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
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

vi.mock('../../api', () => apiMocks);

const superAdmin = accountFixture('superadmin', 'SUPER_ADMIN');
const employee = accountFixture('mario', 'EMPLOYEE');
const auditEvent = auditFixture();
const companySettings = companyFixture();
const systemStatus = systemFixture();

const executorWithoutRefresh: CommandExecutor = async <T,>(options: CommandExecutionOptions<T>) => {
  try {
    const value = await options.command();
    options.applyResponse(value);
    options.afterConfirmed?.(value);
    return { status: 'saved', value };
  } catch (error) {
    return { status: 'failed', error };
  }
};

describe('useAdministrationFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.fetchAccountPage.mockResolvedValue(pageOf([superAdmin, employee]));
    apiMocks.fetchAuditEventPage.mockResolvedValue(pageOf([auditEvent]));
    apiMocks.fetchCompanySettings.mockResolvedValue(companySettings);
    apiMocks.fetchSystemStatus.mockResolvedValue(systemStatus);
    apiMocks.createAccount.mockResolvedValue(employee);
    apiMocks.disableAccount.mockResolvedValue({ ...employee, enabled: false, disabledReason: 'Fine rapporto' });
    apiMocks.enableAccount.mockResolvedValue(employee);
    apiMocks.resetAccountPassword.mockResolvedValue(undefined);
    apiMocks.revokeAccountSessions.mockResolvedValue(undefined);
    apiMocks.changeAccountRole.mockResolvedValue({ ...employee, role: 'ADMIN', roleLabel: 'Admin' });
    apiMocks.updateCompanySettings.mockResolvedValue(companySettings);
  });

  it('carica le risorse amministrative con richieste cancellabili', async () => {
    const { result } = renderAdministrationHook();

    await waitFor(() => expect(result.current.accounts.page.content).toHaveLength(2));
    await waitFor(() => expect(result.current.audit.page.content).toEqual([auditEvent]));
    await waitFor(() => expect(result.current.company.settings).toEqual(companySettings));
    await waitFor(() => expect(result.current.monitoring.status).toEqual(systemStatus));
    expect(apiMocks.fetchAccountPage.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchAuditEventPage.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchCompanySettings.mock.calls[0][0]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchSystemStatus.mock.calls[0][0]).toBeInstanceOf(AbortSignal);
  });

  it('attiva il monitoraggio solo alla prima apertura e conserva lo stato uscendo dalla vista', async () => {
    const { result, rerender } = renderHook(
      ({ active }) => useAdministrationFlow(flowOptions({ monitoringActive: active })),
      { initialProps: { active: false } }
    );

    await waitFor(() => expect(result.current.audit.page.content).toEqual([auditEvent]));
    expect(apiMocks.fetchSystemStatus).not.toHaveBeenCalled();
    rerender({ active: true });
    await waitFor(() => expect(result.current.monitoring.status).toEqual(systemStatus));
    rerender({ active: false });
    expect(result.current.monitoring.status).toEqual(systemStatus);
    expect(apiMocks.fetchSystemStatus).toHaveBeenCalledOnce();
  });

  it('non esegue query amministrative senza i relativi permessi', async () => {
    const { result } = renderAdministrationHook({
      canManageAccounts: false,
      canManageCompanySettings: false,
      canViewAudit: false
    });

    await waitFor(() => expect(result.current.accounts.loading).toBe(false));
    expect(apiMocks.fetchAccountPage).not.toHaveBeenCalled();
    expect(apiMocks.fetchAuditEventPage).not.toHaveBeenCalled();
    expect(apiMocks.fetchCompanySettings).not.toHaveBeenCalled();
    expect(apiMocks.fetchSystemStatus).not.toHaveBeenCalled();
  });

  describe.each([
    ['account', 'fetchAccountPage', { canManageAccounts: true, canManageCompanySettings: false, canViewAudit: false }, (result: FlowResult) => result.accounts.error],
    ['audit', 'fetchAuditEventPage', { canManageAccounts: false, canManageCompanySettings: false, canViewAudit: true, monitoringActive: false }, (result: FlowResult) => result.audit.error],
    ['configurazione', 'fetchCompanySettings', { canManageAccounts: false, canManageCompanySettings: true, canViewAudit: false }, (result: FlowResult) => result.company.error],
    ['monitoraggio', 'fetchSystemStatus', { canManageAccounts: false, canManageCompanySettings: false, canViewAudit: true, monitoringActive: true }, (result: FlowResult) => result.monitoring.error]
  ] as const)('errori risorsa %s', (_, mockName, permissions, readError) => {
    it.each([
      ['401', Object.assign(new Error('Sessione scaduta'), { status: 401 })],
      ['403', Object.assign(new Error('Permesso negato'), { status: 403 })],
      ['500', Object.assign(new Error('Errore server'), { status: 500 })],
      ['offline', new TypeError('Failed to fetch')]
    ])('contiene l errore %s nello stato della slice', async (_status, failure) => {
      apiMocks[mockName].mockRejectedValue(failure);
      const { result } = renderAdministrationHook(permissions);

      await waitFor(() => expect(readError(result.current)).toBe(failure));
    });
  });

  it('mantiene l ultima risposta account quando due ricerche terminano fuori ordine', async () => {
    const first = deferred<PageResponse<UserAccount>>();
    const second = deferred<PageResponse<UserAccount>>();
    apiMocks.fetchAccountPage.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const { result } = renderAdministrationHook({ canManageCompanySettings: false, canViewAudit: false });
    await waitFor(() => expect(apiMocks.fetchAccountPage).toHaveBeenCalledOnce());

    act(() => result.current.accounts.setQuery({ page: 0, size: 8, role: 'EMPLOYEE' }));
    await waitFor(() => expect(apiMocks.fetchAccountPage).toHaveBeenCalledTimes(2));
    expect(apiMocks.fetchAccountPage.mock.calls[0][1].aborted).toBe(true);

    await act(async () => second.resolve(pageOf([employee])));
    await waitFor(() => expect(result.current.accounts.page.content).toEqual([employee]));
    await act(async () => first.resolve(pageOf([superAdmin])));
    expect(result.current.accounts.page.content).toEqual([employee]);
  });

  it('mantiene l ultima risposta audit quando due filtri terminano fuori ordine', async () => {
    const first = deferred<PageResponse<AuditEvent>>();
    const second = deferred<PageResponse<AuditEvent>>();
    const critical = { ...auditEvent, id: 2, severity: 'CRITICAL' as const, action: 'ACCOUNT_DISABLED' };
    apiMocks.fetchAuditEventPage.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const { result } = renderAdministrationHook({ canManageAccounts: false, canManageCompanySettings: false, monitoringActive: false });
    await waitFor(() => expect(apiMocks.fetchAuditEventPage).toHaveBeenCalledOnce());

    act(() => result.current.audit.setQuery({ page: 0, size: 8, severity: 'CRITICAL' }));
    await waitFor(() => expect(apiMocks.fetchAuditEventPage).toHaveBeenCalledTimes(2));
    expect(apiMocks.fetchAuditEventPage.mock.calls[0][1].aborted).toBe(true);

    await act(async () => second.resolve(pageOf([critical])));
    await waitFor(() => expect(result.current.audit.page.content).toEqual([critical]));
    await act(async () => first.resolve(pageOf([auditEvent])));
    expect(result.current.audit.page.content).toEqual([critical]);
  });

  it('crea un account, applica la risposta e pulisce le credenziali solo dopo conferma', async () => {
    const onAccountApplied = vi.fn();
    const { result } = renderAdministrationHook({ canManageCompanySettings: false, canViewAudit: false, onAccountApplied });
    await waitFor(() => expect(result.current.accounts.page.content).toHaveLength(2));
    act(() => {
      result.current.accounts.setForm({ username: 'nuovo', password: 'Password-Sicura-123!', role: 'EMPLOYEE' });
      result.current.accounts.setReauthPassword('Password-Operatore-123!');
    });

    await act(async () => result.current.accounts.create());

    expect(apiMocks.createAccount).toHaveBeenCalledWith('nuovo', 'Password-Sicura-123!', 'EMPLOYEE', 'Password-Operatore-123!');
    expect(onAccountApplied).toHaveBeenCalledWith(employee);
    expect(result.current.accounts.form).toEqual({ username: '', password: '', role: 'EMPLOYEE' });
    expect(result.current.accounts.reauthPassword).toBe('');
  });

  it('preserva form e ri-autenticazione quando la creazione account fallisce', async () => {
    apiMocks.createAccount.mockRejectedValue(new TypeError('Failed to fetch'));
    const { result } = renderAdministrationHook({ canManageCompanySettings: false, canViewAudit: false });
    await waitFor(() => expect(result.current.accounts.page.content).toHaveLength(2));
    const form = { username: 'nuovo', password: 'Password-Sicura-123!', role: 'EMPLOYEE' as const };
    act(() => {
      result.current.accounts.setForm(form);
      result.current.accounts.setReauthPassword('Password-Operatore-123!');
    });

    await act(async () => result.current.accounts.create());

    expect(result.current.accounts.form).toEqual(form);
    expect(result.current.accounts.reauthPassword).toBe('Password-Operatore-123!');
  });

  it('blocca il comando account privo di ri-autenticazione', async () => {
    const onNotice = vi.fn();
    const { result } = renderAdministrationHook({ canManageCompanySettings: false, canViewAudit: false, onNotice });
    await waitFor(() => expect(result.current.accounts.page.content).toHaveLength(2));
    act(() => result.current.accounts.setForm({ username: 'nuovo', password: 'Password-Sicura-123!', role: 'EMPLOYEE' }));

    await act(async () => result.current.accounts.create());

    expect(apiMocks.createAccount).not.toHaveBeenCalled();
    expect(onNotice).toHaveBeenCalledWith('Inserisci la password della sessione per gestire gli account.', 'error', 'Operazione non riuscita');
  });

  it('applica il lifecycle account e mantiene sincronizzato il lookup partner', async () => {
    const onAccountApplied = vi.fn();
    const { result } = renderAdministrationHook({ canManageCompanySettings: false, canViewAudit: false, onAccountApplied });
    await waitFor(() => expect(result.current.accounts.page.content).toHaveLength(2));
    act(() => result.current.accounts.setReauthPassword('Password-Operatore-123!'));

    await act(async () => result.current.accounts.disable('mario', 'Fine rapporto'));

    expect(apiMocks.disableAccount).toHaveBeenCalledWith('mario', 'Fine rapporto', 'Password-Operatore-123!');
    expect(result.current.accounts.page.content.find((account) => account.username === 'mario')?.enabled).toBe(false);
    expect(onAccountApplied).toHaveBeenCalledWith(expect.objectContaining({ username: 'mario', enabled: false }));
    expect(result.current.accounts.reauthPassword).toBe('');
  });

  it('inoltra reset password, revoca sessioni e cambio ruolo con la ri-autenticazione', async () => {
    const { result } = renderAdministrationHook({ canManageCompanySettings: false, canViewAudit: false });
    await waitFor(() => expect(result.current.accounts.page.content).toHaveLength(2));

    act(() => result.current.accounts.setReauthPassword('Password-Operatore-123!'));
    await act(async () => result.current.accounts.resetPassword('mario', 'Nuova-Password-123!', 'Reset autorizzato'));
    act(() => result.current.accounts.setReauthPassword('Password-Operatore-123!'));
    await act(async () => result.current.accounts.revokeSessions('mario', 'Sessione compromessa'));
    act(() => result.current.accounts.setReauthPassword('Password-Operatore-123!'));
    await act(async () => result.current.accounts.changeRole('mario', 'ADMIN', 'Promozione'));

    expect(apiMocks.resetAccountPassword).toHaveBeenCalledWith('mario', 'Nuova-Password-123!', 'Reset autorizzato', 'Password-Operatore-123!');
    expect(apiMocks.revokeAccountSessions).toHaveBeenCalledWith('mario', 'Sessione compromessa', 'Password-Operatore-123!');
    expect(apiMocks.changeAccountRole).toHaveBeenCalledWith('mario', 'ADMIN', 'Promozione', 'Password-Operatore-123!');
  });

  it('protegge sessione corrente, super admin e admin gestiti da un admin ordinario', async () => {
    const admin = accountFixture('admin', 'ADMIN');
    const anotherAdmin = accountFixture('altro-admin', 'ADMIN');
    const { result } = renderAdministrationHook({ currentUser: admin, canManageCompanySettings: false, canViewAudit: false });
    await waitFor(() => expect(result.current.accounts.page.content).toHaveLength(2));

    expect(result.current.accounts.canManage(admin)).toBe(false);
    expect(result.current.accounts.protectionLabel(admin)).toBe('Sessione attiva');
    expect(result.current.accounts.canManage(superAdmin)).toBe(false);
    expect(result.current.accounts.protectionLabel(superAdmin)).toBe('Protetto');
    expect(result.current.accounts.canManage(anotherAdmin)).toBe(false);
    expect(result.current.accounts.protectionLabel(anotherAdmin)).toBe('Solo super admin');
    expect(result.current.accounts.canManage(employee)).toBe(true);
  });

  it('salva la configurazione applicando la risposta autorevole', async () => {
    const updated = { ...companySettings, legalName: 'Azienda Aggiornata', version: 2 };
    apiMocks.updateCompanySettings.mockResolvedValue(updated);
    const { result } = renderAdministrationHook({ canManageAccounts: false, canViewAudit: false });
    await waitFor(() => expect(result.current.company.settings).toEqual(companySettings));

    await act(async () => result.current.company.save(companySettings));

    expect(apiMocks.updateCompanySettings).toHaveBeenCalledWith(companySettings);
    expect(result.current.company.settings).toEqual(updated);
  });
});

type FlowOverrides = Partial<Parameters<typeof useAdministrationFlow>[0]>;
type FlowResult = ReturnType<typeof useAdministrationFlow>;

function renderAdministrationHook(overrides: FlowOverrides = {}) {
  return renderHook(() => useAdministrationFlow(flowOptions(overrides)));
}

function flowOptions(overrides: FlowOverrides = {}): Parameters<typeof useAdministrationFlow>[0] {
  return {
    enabled: true,
    currentUser: superAdmin,
    canManageAccounts: true,
    canManageCompanySettings: true,
    canViewAudit: true,
    monitoringActive: true,
    executeCommand: executorWithoutRefresh,
    onAccountApplied: vi.fn(),
    onRefreshCustomerAccounts: vi.fn().mockResolvedValue(undefined),
    onNotice: vi.fn(),
    ...overrides
  };
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

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function accountFixture(username: string, role: UserAccount['role']): UserAccount {
  return {
    id: username.length,
    username,
    role,
    roleLabel: role === 'SUPER_ADMIN' ? 'Super admin' : role === 'ADMIN' ? 'Admin' : role === 'EMPLOYEE' ? 'Dipendente' : 'Cliente',
    permissions: role === 'SUPER_ADMIN' ? ['MANAGE_ACCOUNTS', 'MANAGE_COMPANY_SETTINGS', 'VIEW_AUDIT'] : role === 'ADMIN' ? ['MANAGE_ACCOUNTS'] : [],
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
