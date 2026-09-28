import { useMemo, useState } from 'react';
import {
  BusinessPartner,
  BusinessPartnerPayload,
  createPartner,
  deactivatePartner,
  fetchAccounts,
  fetchPartnerPage,
  linkPartnerAccount,
  PageResponse,
  PartnerQuery,
  unlinkPartnerAccount,
  updatePartner,
  UserAccount
} from '../../api';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import useDraftState from '../../hooks/useDraftState';
import { commandWasSaved, type CommandExecutor } from '../../hooks/useCommandExecution';
import usePaginatedResource from '../../hooks/usePaginatedResource';
import { removePageItems, upsertPageItem } from '../../utils/pageState';

const DEFAULT_PAGE_SIZE = 8;
const INITIAL_QUERY: PartnerQuery = { page: 0, size: DEFAULT_PAGE_SIZE, active: true };
const CUSTOMER_ACCOUNT_QUERY = { role: 'CUSTOMER' as const, enabled: true };
const EMPTY_FORM: BusinessPartnerPayload = {
  code: '',
  type: 'CUSTOMER',
  displayName: '',
  taxCode: '',
  vatNumber: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  notes: ''
};

type PartnerFlowOptions = {
  enabled: boolean;
  canLinkAccounts: boolean;
  executeCommand: CommandExecutor;
};

export default function usePartnerFlow({ enabled, canLinkAccounts, executeCommand }: PartnerFlowOptions) {
  const [query, setQuery] = useState<PartnerQuery>(INITIAL_QUERY);
  const [editingPartner, setEditingPartner] = useState<BusinessPartner | null>(null);
  const initialForm = useMemo(() => editingPartner ? partnerToForm(editingPartner) : EMPTY_FORM, [editingPartner]);
  const { value: form, setValue: setForm, dirty: formDirty, clear: clearPartnerDraft } = useDraftState({
    key: `partners:${editingPartner?.id ?? 'new'}`,
    view: 'partners',
    label: editingPartner ? `Modifica anagrafica ${editingPartner.code}` : 'Nuova anagrafica',
    initialValue: initialForm
  });
  const debouncedSearch = useDebouncedValue(query.q ?? '', 300);
  const requestQuery = useMemo<PartnerQuery>(() => ({ ...query, q: debouncedSearch }), [debouncedSearch, query]);
  const emptyPartnerPage = useMemo(() => emptyPage<BusinessPartner>(), []);
  const emptyCustomerAccounts = useMemo<UserAccount[]>(() => [], []);

  const partners = usePaginatedResource({
    query: requestQuery,
    enabled,
    initialData: emptyPartnerPage,
    loader: fetchPartnerPage
  });
  const customerAccounts = usePaginatedResource({
    query: CUSTOMER_ACCOUNT_QUERY,
    enabled: enabled && canLinkAccounts,
    initialData: emptyCustomerAccounts,
    loader: loadCustomerAccounts
  });

  function applyPartner(partner: BusinessPartner, previousCode?: string) {
    partners.setData((page) => upsertPageItem(page, partner, (item) => item.code, previousCode));
    setEditingPartner((current) => current?.code === (previousCode ?? partner.code) ? partner : current);
  }

  function applyCustomerAccount(account: UserAccount) {
    customerAccounts.setData((accounts) => {
      if (account.role !== 'CUSTOMER' || !account.enabled) {
        return accounts.filter((item) => item.id !== account.id);
      }
      const index = accounts.findIndex((item) => item.id === account.id);
      if (index < 0) return [account, ...accounts];
      const updated = [...accounts];
      updated[index] = account;
      return updated;
    });
  }

  async function submitPartner(): Promise<boolean> {
    const submittedForm = { ...form };
    const previousCode = editingPartner?.code;
    const result = await executeCommand({
      key: `partner:${previousCode ? 'update' : 'create'}:${previousCode ?? submittedForm.code}`,
      command: () => previousCode ? updatePartner(previousCode, submittedForm) : createPartner(submittedForm),
      applyResponse: (partner) => applyPartner(partner, previousCode),
      afterConfirmed: clearForm,
      refresh: refreshPartners,
      successMessage: previousCode ? `Anagrafica ${submittedForm.code} aggiornata.` : `Anagrafica ${submittedForm.code} creata.`
    });
    return commandWasSaved(result);
  }

  async function deactivate(code: string) {
    if (!window.confirm(`Disattivare l'anagrafica ${code}?`)) return;
    await executeCommand({
      key: `partner:deactivate:${code}`,
      command: () => deactivatePartner(code),
      applyResponse: () => partners.setData((page) => query.active === true
        ? removePageItems(page, new Set([code]), (partner) => partner.code)
        : { ...page, content: page.content.map((partner) => partner.code === code ? { ...partner, active: false } : partner) }),
      afterConfirmed: () => {
        if (editingPartner?.code === code) clearForm();
      },
      refresh: refreshPartners,
      successMessage: `Anagrafica ${code} disattivata.`
    });
  }

  async function linkAccount(code: string, accountId: number) {
    await executeCommand({
      key: `partner:link:${code}`,
      command: () => linkPartnerAccount(code, accountId),
      applyResponse: applyPartner,
      refresh: refreshPartners,
      successMessage: 'Account cliente collegato all\'anagrafica. I nuovi ordini useranno l\'ownership stabile.'
    });
  }

  async function unlinkAccount(code: string) {
    await executeCommand({
      key: `partner:unlink:${code}`,
      command: () => unlinkPartnerAccount(code),
      applyResponse: applyPartner,
      refresh: refreshPartners,
      successMessage: 'Collegamento account rimosso. Gli ordini storici mantengono il proprio riferimento.'
    });
  }

  function editPartner(partner: BusinessPartner) {
    setEditingPartner(partner);
  }

  function clearForm() {
    clearPartnerDraft();
    setEditingPartner(null);
  }

  async function refreshPartners() {
    await partners.refresh();
  }

  async function refreshCustomerAccounts() {
    if (!canLinkAccounts) return;
    await customerAccounts.refresh();
  }

  async function refreshResources() {
    await Promise.all([refreshPartners(), refreshCustomerAccounts()]);
  }

  function reset() {
    setQuery(INITIAL_QUERY);
    clearForm();
    partners.setData(emptyPartnerPage);
    customerAccounts.setData(emptyCustomerAccounts);
  }

  return {
    query,
    page: partners.data,
    form,
    formDirty,
    editingPartner,
    customerAccounts: customerAccounts.data,
    loading: partners.loading || customerAccounts.loading,
    refreshing: partners.refreshing || customerAccounts.refreshing,
    error: partners.error ?? customerAccounts.error,
    pageSize: DEFAULT_PAGE_SIZE,
    setQuery,
    setForm,
    editPartner,
    cancelEdit: clearForm,
    submitPartner,
    deactivate,
    linkAccount,
    unlinkAccount,
    applyCustomerAccount,
    refreshPartners,
    refreshCustomerAccounts,
    refreshResources,
    reset
  };
}

export type PartnerFlowController = ReturnType<typeof usePartnerFlow>;

function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: DEFAULT_PAGE_SIZE, totalElements: 0, totalPages: 0, first: true, last: true };
}

function loadCustomerAccounts(_: typeof CUSTOMER_ACCOUNT_QUERY, signal: AbortSignal) {
  return fetchAccounts(CUSTOMER_ACCOUNT_QUERY, signal);
}

function partnerToForm(partner: BusinessPartner): BusinessPartnerPayload {
  return {
    code: partner.code,
    type: partner.type,
    displayName: partner.displayName,
    taxCode: partner.taxCode,
    vatNumber: partner.vatNumber,
    email: partner.email,
    phone: partner.phone,
    address: partner.address,
    city: partner.city,
    notes: partner.notes
  };
}
