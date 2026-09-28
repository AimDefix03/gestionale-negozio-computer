import { useEffect, useMemo, useState } from 'react';
import {
  AccountQuery,
  AuditEvent,
  AuditQuery,
  changeAccountRole,
  CompanySettings,
  CompanySettingsPayload,
  createAccount,
  disableAccount,
  enableAccount,
  fetchAccountPage,
  fetchAuditEventPage,
  fetchCompanySettings,
  fetchSystemStatus,
  PageResponse,
  resetAccountPassword,
  revokeAccountSessions,
  SystemStatus,
  updateCompanySettings,
  UserAccount,
  UserRole
} from '../../api';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import { commandWasSaved, type CommandExecutor, type NoticeTone } from '../../hooks/useCommandExecution';
import usePaginatedResource from '../../hooks/usePaginatedResource';
import type { AccountFormState } from '../../types/ui';
import { upsertPageItem } from '../../utils/pageState';

const DEFAULT_PAGE_SIZE = 8;
const INITIAL_ACCOUNT_QUERY: AccountQuery = { page: 0, size: DEFAULT_PAGE_SIZE };
const INITIAL_AUDIT_QUERY: AuditQuery = { page: 0, size: DEFAULT_PAGE_SIZE };
const INITIAL_ACCOUNT_FORM: AccountFormState = { username: '', password: '', role: 'EMPLOYEE' };
const COMPANY_RESOURCE = 'company-settings';
const SYSTEM_RESOURCE = 'system-status';

type AdministrationFlowOptions = {
  enabled: boolean;
  currentUser: UserAccount | null;
  canManageAccounts: boolean;
  canManageCompanySettings: boolean;
  canViewAudit: boolean;
  monitoringActive: boolean;
  executeCommand: CommandExecutor;
  onAccountApplied: (account: UserAccount) => void;
  onRefreshCustomerAccounts: () => Promise<unknown>;
  onNotice: (message: string, tone?: NoticeTone, title?: string) => void;
};

export default function useAdministrationFlow({
  enabled,
  currentUser,
  canManageAccounts,
  canManageCompanySettings,
  canViewAudit,
  monitoringActive,
  executeCommand,
  onAccountApplied,
  onRefreshCustomerAccounts,
  onNotice
}: AdministrationFlowOptions) {
  const [accountQuery, setAccountQuery] = useState<AccountQuery>(INITIAL_ACCOUNT_QUERY);
  const [auditQuery, setAuditQuery] = useState<AuditQuery>(INITIAL_AUDIT_QUERY);
  const [accountForm, setAccountForm] = useState<AccountFormState>(INITIAL_ACCOUNT_FORM);
  const [reauthPassword, setReauthPassword] = useState('');
  const [monitoringActivated, setMonitoringActivated] = useState(false);
  const debouncedAccountSearch = useDebouncedValue(accountQuery.q ?? '', 300);
  const debouncedAuditSearch = useDebouncedValue(auditQuery.q ?? '', 300);
  const accountRequestQuery = useMemo<AccountQuery>(() => ({
    ...accountQuery,
    q: debouncedAccountSearch
  }), [accountQuery, debouncedAccountSearch]);
  const auditRequestQuery = useMemo<AuditQuery>(() => ({
    ...auditQuery,
    q: debouncedAuditSearch
  }), [auditQuery, debouncedAuditSearch]);
  const emptyAccounts = useMemo(() => emptyPage<UserAccount>(), []);
  const emptyAudit = useMemo(() => emptyPage<AuditEvent>(), []);

  useEffect(() => {
    if (!enabled || !canViewAudit) {
      setMonitoringActivated(false);
      return;
    }
    if (monitoringActive) setMonitoringActivated(true);
  }, [canViewAudit, enabled, monitoringActive]);

  const accounts = usePaginatedResource({
    query: accountRequestQuery,
    enabled: enabled && canManageAccounts,
    initialData: emptyAccounts,
    loader: fetchAccountPage
  });
  const audit = usePaginatedResource({
    query: auditRequestQuery,
    enabled: enabled && canViewAudit,
    initialData: emptyAudit,
    loader: fetchAuditEventPage
  });
  const company = usePaginatedResource({
    query: COMPANY_RESOURCE,
    enabled: enabled && canManageCompanySettings,
    initialData: null as CompanySettings | null,
    loader: loadCompanySettings
  });
  const monitoring = usePaginatedResource({
    query: SYSTEM_RESOURCE,
    enabled: enabled && canViewAudit && monitoringActivated,
    initialData: null as SystemStatus | null,
    loader: loadSystemStatus
  });

  function applyAccount(account: UserAccount) {
    accounts.setData((page) => upsertPageItem(page, account, (item) => item.username));
    onAccountApplied(account);
  }

  async function createNewAccount() {
    if (!currentUser) return;
    if (!requireReauthentication('Inserisci la password della sessione per gestire gli account.')) return;
    const submittedAccount = { ...accountForm };
    const submittedReauthPassword = reauthPassword;
    await executeCommand({
      key: `account:create:${submittedAccount.username}`,
      command: () => createAccount(submittedAccount.username, submittedAccount.password, submittedAccount.role, submittedReauthPassword),
      applyResponse: applyAccount,
      afterConfirmed: () => {
        setAccountForm(INITIAL_ACCOUNT_FORM);
        setReauthPassword('');
      },
      refresh: refreshAccountsAndPartners,
      successMessage: `Account ${submittedAccount.username} creato.`
    });
  }

  async function changeAccountState(username: string, reason: string, accountEnabled: boolean) {
    if (!requireReauthentication('Inserisci la password della sessione per gestire lo stato account.')) return;
    const submittedReauthPassword = reauthPassword;
    await executeCommand({
      key: `account:${accountEnabled ? 'enable' : 'disable'}:${username}`,
      command: () => accountEnabled
        ? enableAccount(username, reason, submittedReauthPassword)
        : disableAccount(username, reason, submittedReauthPassword),
      applyResponse: applyAccount,
      afterConfirmed: () => setReauthPassword(''),
      refresh: refreshAccountsAndPartners,
      successMessage: `Accesso di ${username} ${accountEnabled ? 'riabilitato' : 'disattivato'}.`
    });
  }

  async function resetPassword(username: string, password: string, reason: string) {
    if (!requireReauthentication('Inserisci la password della sessione per reimpostare le credenziali.')) return;
    const submittedReauthPassword = reauthPassword;
    await executeCommand({
      key: `account:password-reset:${username}`,
      command: () => resetAccountPassword(username, password, reason, submittedReauthPassword),
      applyResponse: () => undefined,
      afterConfirmed: () => setReauthPassword(''),
      refresh: refreshAccounts,
      successMessage: `Password di ${username} reimpostata e sessioni revocate.`
    });
  }

  async function revokeSessions(username: string, reason: string) {
    if (!requireReauthentication('Inserisci la password della sessione per revocare le sessioni.')) return;
    const submittedReauthPassword = reauthPassword;
    await executeCommand({
      key: `account:revoke-sessions:${username}`,
      command: () => revokeAccountSessions(username, reason, submittedReauthPassword),
      applyResponse: () => undefined,
      afterConfirmed: () => setReauthPassword(''),
      successMessage: `Tutte le sessioni di ${username} sono state revocate.`
    });
  }

  async function changeRole(username: string, role: UserRole, reason: string) {
    if (!requireReauthentication('Inserisci la password della sessione per modificare il ruolo.')) return;
    const submittedReauthPassword = reauthPassword;
    await executeCommand({
      key: `account:role:${username}:${role}`,
      command: () => changeAccountRole(username, role, reason, submittedReauthPassword),
      applyResponse: applyAccount,
      afterConfirmed: () => setReauthPassword(''),
      refresh: refreshAccountsAndPartners,
      successMessage: `Ruolo di ${username} aggiornato.`
    });
  }

  async function saveCompanySettings(payload: CompanySettingsPayload) {
    const result = await executeCommand({
      key: 'company-settings:update',
      command: () => updateCompanySettings(payload),
      applyResponse: company.setData,
      successMessage: 'Configurazione aziendale aggiornata. I nuovi documenti useranno questi dati.'
    });
    return commandWasSaved(result);
  }

  function canManageAccount(account: UserAccount) {
    if (!currentUser || !canManageAccounts) return false;
    if (account.username.toLowerCase() === currentUser.username.toLowerCase()) return false;
    if (account.role === 'SUPER_ADMIN') return false;
    if (account.role === 'ADMIN' && currentUser.role !== 'SUPER_ADMIN') return false;
    return true;
  }

  function accountProtectionLabel(account: UserAccount) {
    if (currentUser && account.username.toLowerCase() === currentUser.username.toLowerCase()) return 'Sessione attiva';
    if (account.role === 'SUPER_ADMIN') return 'Protetto';
    if (account.role === 'ADMIN' && currentUser?.role !== 'SUPER_ADMIN') return 'Solo super admin';
    return 'Non disponibile';
  }

  function requireReauthentication(message: string) {
    if (reauthPassword) return true;
    onNotice(message, 'error', 'Operazione non riuscita');
    return false;
  }

  async function refreshAccounts() {
    await accounts.refresh();
  }

  async function refreshAccountsAndPartners() {
    await Promise.all([refreshAccounts(), onRefreshCustomerAccounts()]);
  }

  async function refreshAudit() {
    await audit.refresh();
  }

  async function refreshCompany() {
    await company.refresh();
  }

  async function refreshMonitoring() {
    await monitoring.refresh();
  }

  async function refreshResources() {
    await Promise.all([refreshAccounts(), refreshAudit(), refreshCompany(), refreshMonitoring()]);
  }

  function reset() {
    setAccountQuery(INITIAL_ACCOUNT_QUERY);
    setAuditQuery(INITIAL_AUDIT_QUERY);
    setAccountForm(INITIAL_ACCOUNT_FORM);
    setReauthPassword('');
    setMonitoringActivated(false);
    accounts.setData(emptyAccounts);
    audit.setData(emptyAudit);
    company.setData(null);
    monitoring.setData(null);
  }

  return {
    accounts: {
      query: accountQuery,
      page: accounts.data,
      form: accountForm,
      reauthPassword,
      loading: accounts.loading,
      refreshing: accounts.refreshing,
      error: accounts.error,
      pageSize: DEFAULT_PAGE_SIZE,
      setQuery: setAccountQuery,
      setForm: setAccountForm,
      setReauthPassword,
      create: createNewAccount,
      disable: (username: string, reason: string) => changeAccountState(username, reason, false),
      enable: (username: string, reason: string) => changeAccountState(username, reason, true),
      resetPassword,
      revokeSessions,
      changeRole,
      canManage: canManageAccount,
      protectionLabel: accountProtectionLabel,
      refresh: refreshAccounts
    },
    audit: {
      query: auditQuery,
      page: audit.data,
      loading: audit.loading,
      refreshing: audit.refreshing,
      error: audit.error,
      pageSize: DEFAULT_PAGE_SIZE,
      setQuery: setAuditQuery,
      refresh: refreshAudit
    },
    company: {
      settings: company.data,
      loading: company.loading,
      refreshing: company.refreshing,
      error: company.error,
      save: saveCompanySettings,
      refresh: refreshCompany
    },
    monitoring: {
      status: monitoring.data,
      loading: monitoring.loading,
      refreshing: monitoring.refreshing,
      error: monitoring.error,
      refresh: refreshMonitoring
    },
    refreshResources,
    reset
  };
}

export type AdministrationFlowController = ReturnType<typeof useAdministrationFlow>;

function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: DEFAULT_PAGE_SIZE, totalElements: 0, totalPages: 0, first: true, last: true };
}

function loadCompanySettings(_: string, signal: AbortSignal) {
  return fetchCompanySettings(signal);
}

function loadSystemStatus(_: string, signal: AbortSignal) {
  return fetchSystemStatus(signal);
}
