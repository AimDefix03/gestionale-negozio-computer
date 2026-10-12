import { act, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { BusinessPartner, PageResponse, UserAccount } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import usePartnerFlow from './usePartnerFlow';
import { renderHookWithDrafts as renderHook } from '../../test/renderWithDrafts';

const apiMocks = vi.hoisted(() => ({
  createPartner: vi.fn(),
  updatePartner: vi.fn(),
  deactivatePartner: vi.fn(),
  fetchAccounts: vi.fn(),
  fetchPartnerPage: vi.fn(),
  linkPartnerAccount: vi.fn(),
  unlinkPartnerAccount: vi.fn()
}));

vi.mock('../../api', () => apiMocks);

const partner: BusinessPartner = {
  id: 1,
  code: 'CLI-001',
  type: 'CUSTOMER',
  typeLabel: 'Cliente',
  displayName: 'Cliente Test',
  taxCode: 'TSTCLN80A01F839A',
  vatNumber: '',
  email: 'cliente@example.test',
  phone: '',
  address: 'Via Test 1',
  city: 'Napoli',
  notes: '',
  active: true,
  createdAt: '2026-08-18T10:00:00Z',
  updatedAt: '2026-08-18T10:00:00Z'
};

const customerAccount: UserAccount = {
  id: 42,
  username: 'cliente.test',
  role: 'CUSTOMER',
  roleLabel: 'Cliente',
  permissions: [],
  enabled: true,
  disabledAt: null,
  disabledBy: null,
  disabledReason: null
};

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

describe('usePartnerFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('confirm', vi.fn(() => true));
    apiMocks.fetchPartnerPage.mockResolvedValue(pageOf([partner]));
    apiMocks.fetchAccounts.mockResolvedValue([customerAccount]);
    apiMocks.createPartner.mockResolvedValue(partner);
    apiMocks.updatePartner.mockResolvedValue(partner);
    apiMocks.deactivatePartner.mockResolvedValue(undefined);
    apiMocks.linkPartnerAccount.mockResolvedValue({ ...partner, linkedAccountId: customerAccount.id });
    apiMocks.unlinkPartnerAccount.mockResolvedValue({ ...partner, linkedAccountId: undefined });
  });

  it('carica anagrafiche e account cliente con richieste cancellabili', async () => {
    const { result } = renderPartnerHook();

    await waitFor(() => expect(result.current.page.content).toEqual([partner]));
    await waitFor(() => expect(result.current.customerAccounts).toEqual([customerAccount]));
    expect(apiMocks.fetchPartnerPage.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchAccounts.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
  });

  it('non carica gli account cliente senza il permesso amministrativo', async () => {
    const { result } = renderPartnerHook({ canLinkAccounts: false });

    await waitFor(() => expect(result.current.page.content).toEqual([partner]));
    expect(apiMocks.fetchAccounts).not.toHaveBeenCalled();
    expect(result.current.customerAccounts).toEqual([]);
  });

  it.each([
    ['401', Object.assign(new Error('Sessione scaduta'), { status: 401 })],
    ['403', Object.assign(new Error('Permesso negato'), { status: 403 })],
    ['500', Object.assign(new Error('Errore server'), { status: 500 })],
    ['offline', new TypeError('Failed to fetch')]
  ])('contiene l errore %s nello stato della slice', async (_, failure) => {
    apiMocks.fetchPartnerPage.mockRejectedValue(failure);
    const { result } = renderPartnerHook();

    await waitFor(() => expect(result.current.error).toBe(failure));
    expect(result.current.loading).toBe(false);
  });

  it('mantiene la risposta piu recente quando due filtri terminano fuori ordine', async () => {
    const first = deferred<PageResponse<BusinessPartner>>();
    const second = deferred<PageResponse<BusinessPartner>>();
    const supplier = { ...partner, id: 2, code: 'FOR-002', type: 'SUPPLIER' as const, typeLabel: 'Fornitore', displayName: 'Fornitore Test' };
    apiMocks.fetchPartnerPage.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const { result } = renderPartnerHook();
    await waitFor(() => expect(apiMocks.fetchPartnerPage).toHaveBeenCalledOnce());

    act(() => result.current.setQuery({ page: 0, size: 8, active: true, type: 'SUPPLIER' }));
    await waitFor(() => expect(apiMocks.fetchPartnerPage).toHaveBeenCalledTimes(2));
    expect(apiMocks.fetchPartnerPage.mock.calls[0][1].aborted).toBe(true);

    await act(async () => second.resolve(pageOf([supplier])));
    await waitFor(() => expect(result.current.page.content[0].code).toBe('FOR-002'));
    await act(async () => first.resolve(pageOf([partner])));
    expect(result.current.page.content[0].code).toBe('FOR-002');
  });

  it('mappa la modifica, applica la risposta e pulisce il form dopo conferma', async () => {
    const updated = { ...partner, displayName: 'Cliente Aggiornato' };
    apiMocks.updatePartner.mockResolvedValue(updated);
    const { result } = renderPartnerHook();
    await waitFor(() => expect(result.current.page.content).toEqual([partner]));
    act(() => result.current.editPartner(partner));
    expect(result.current.form.displayName).toBe(partner.displayName);
    act(() => result.current.setForm({ ...result.current.form, displayName: updated.displayName }));

    let saved = false;
    await act(async () => {
      saved = await result.current.submitPartner();
    });

    expect(saved).toBe(true);
    expect(apiMocks.updatePartner).toHaveBeenCalledWith(partner.code, expect.objectContaining({ displayName: updated.displayName }));
    expect(result.current.page.content[0].displayName).toBe(updated.displayName);
    expect(result.current.editingPartner).toBeNull();
    expect(result.current.form.code).toBe('');
  });

  it('preserva il form quando la creazione fallisce', async () => {
    apiMocks.createPartner.mockRejectedValue(new Error('offline'));
    const { result } = renderPartnerHook();
    await waitFor(() => expect(result.current.page.content).toEqual([partner]));
    act(() => result.current.setForm({ ...result.current.form, code: 'CLI-NEW', displayName: 'Nuovo cliente' }));

    let saved = true;
    await act(async () => {
      saved = await result.current.submitPartner();
    });

    expect(saved).toBe(false);
    expect(result.current.form).toMatchObject({ code: 'CLI-NEW', displayName: 'Nuovo cliente' });
    expect(result.current.page.content).toEqual([partner]);
  });

  it('rimuove localmente un anagrafica attiva dopo la disattivazione', async () => {
    const { result } = renderPartnerHook();
    await waitFor(() => expect(result.current.page.content).toEqual([partner]));
    act(() => result.current.editPartner(partner));

    await act(async () => result.current.deactivate(partner.code));

    expect(apiMocks.deactivatePartner).toHaveBeenCalledWith(partner.code);
    expect(result.current.page.content).toEqual([]);
    expect(result.current.editingPartner).toBeNull();
  });

  it('collega e scollega l account applicando le risposte autorevoli', async () => {
    const { result } = renderPartnerHook();
    await waitFor(() => expect(result.current.page.content).toEqual([partner]));
    act(() => result.current.editPartner(partner));

    await act(async () => result.current.linkAccount(partner.code, customerAccount.id));
    expect(result.current.page.content[0].linkedAccountId).toBe(customerAccount.id);
    await act(async () => result.current.unlinkAccount(partner.code));
    expect(result.current.page.content[0].linkedAccountId).toBeUndefined();
    expect(apiMocks.linkPartnerAccount).toHaveBeenCalledWith(partner.code, customerAccount.id);
    expect(apiMocks.unlinkPartnerAccount).toHaveBeenCalledWith(partner.code);
  });

  it('mantiene il lookup coerente quando un account cliente viene disabilitato', async () => {
    const { result } = renderPartnerHook();
    await waitFor(() => expect(result.current.customerAccounts).toEqual([customerAccount]));

    act(() => result.current.applyCustomerAccount({ ...customerAccount, enabled: false }));

    expect(result.current.customerAccounts).toEqual([]);
  });
});

function renderPartnerHook({ canLinkAccounts = true }: { canLinkAccounts?: boolean } = {}) {
  return renderHook(() => usePartnerFlow({
    enabled: true,
    canLinkAccounts,
    executeCommand: executorWithoutRefresh
  }));
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
