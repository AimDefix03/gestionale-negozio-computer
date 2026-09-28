import { act, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { BusinessPartner, FinancialReconciliation, Order, PageResponse, Product, UserAccount } from '../../api';
import type { CommandExecutionOptions, CommandExecutor, CommandOutcome } from '../../hooks/useCommandExecution';
import useOrderFlow from './useOrderFlow';
import { renderHookWithDrafts as renderHook } from '../../test/renderWithDrafts';

const apiMocks = vi.hoisted(() => ({
  approveOrderReturn: vi.fn(),
  cancelOrder: vi.fn(),
  confirmOrder: vi.fn(),
  createOrder: vi.fn(),
  fetchFinancialReconciliation: vi.fn(),
  fetchOrderPage: vi.fn(),
  fetchOrderDetail: vi.fn(),
  fetchPartnerPage: vi.fn(),
  fulfillOrder: vi.fn(),
  receiveOrderReturn: vi.fn(),
  recordOrderReceipt: vi.fn(),
  refundOrderReturn: vi.fn(),
  rejectOrderReturn: vi.fn(),
  requestOrderReturn: vi.fn()
}));

vi.mock('../../api', () => apiMocks);

const product: Product = {
  id: 1,
  code: 'GPU-001',
  name: 'Scheda video',
  description: 'Prodotto di test',
  category: 'HARDWARE',
  brand: 'Example Brand',
  productType: 'GPU',
  usageContext: 'Gaming',
  quantity: 5,
  reservedQuantity: 1,
  availableQuantity: 4,
  lastPurchaseCost: 500, averagePurchaseCost: 500, costedQuantity: 5, uncostedQuantity: 0, costCoveragePercentage: 100, knownInventoryCost: 2500, potentialGrossMarginOnCostedStock: 1550,
  price: 900,
  discount: 10,
  discountedPrice: 810,
  discontinued: false,
  capabilities: { canEdit: true, canChangeCode: true, canDelete: true, canDiscontinue: true, canMoveStock: true }
};

const partner: BusinessPartner = {
  id: 42,
  code: 'CLI-001',
  type: 'CUSTOMER',
  typeLabel: 'Cliente',
  displayName: 'Cliente censito',
  taxCode: '',
  vatNumber: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  notes: '',
  active: true,
  createdAt: '2026-08-18T09:00:00Z',
  updatedAt: '2026-08-18T09:00:00Z'
};

const order: Order = {
  id: 1,
  code: 'ORD-0001',
  customer: partner.displayName,
  customerCode: partner.code,
  partnerId: partner.id,
  customerType: 'REGISTERED',
  customerTypeLabel: 'Cliente censito',
  ownershipStatus: 'PARTNER',
  ownershipStatusLabel: 'Anagrafica cliente',
  timestamp: '2026-08-18T09:00:00Z',
  paymentMethod: 'Carta',
  total: 810,
  status: 'DRAFT',
  statusLabel: 'Bozza',
  statusChangedAt: '2026-08-18T09:00:00Z',
  cancellationReference: null,
  cancellationReason: null,
  canceledAt: null,
  canceledBy: null,
  canceledByRole: null,
  items: [{ productCode: product.code, productName: product.name, productDescription: product.description, quantity: 1, returnedOrReservedQuantity: 0, returnableQuantity: 1, unitPrice: 810, lineTotal: 810 }],
  returns: [],
  payment: {
    id: 1,
    method: 'CARD',
    methodLabel: 'Carta',
    methodDetails: '',
    status: 'PENDING',
    statusLabel: 'In attesa',
    requestedAmount: 810,
    paidAmount: 0,
    refundedAmount: 0,
    netPaidAmount: 0,
    outstandingAmount: 810,
    refundableAmount: 0,
    currency: 'EUR',
    createdAt: '2026-08-18T09:00:00Z',
    updatedAt: '2026-08-18T09:00:00Z',
    reconciliationRequired: false,
    reconciledAt: null,
    reconciledBy: null,
    reconciledByRole: null,
    reconciliationReference: null,
    reconciliationReason: null,
    transactions: []
  },
  capabilities: { canConfirm: true, canFulfill: false, canCancel: true, canRecordReceipt: false, canRequestReturn: false, returns: [] }
};

const financialReconciliation: FinancialReconciliation = {
  generatedAt: '2026-08-18T09:00:00Z',
  balanced: true,
  checkedPayments: 1,
  checkedReturns: 0,
  mismatchCount: 0,
  counts: {},
  mismatches: []
};

const staff: UserAccount = {
  id: 10,
  username: 'operatore',
  role: 'EMPLOYEE',
  roleLabel: 'Dipendente',
  permissions: ['VIEW_ORDERS', 'CREATE_ORDERS', 'CONFIRM_ORDERS', 'FULFILL_ORDERS', 'CANCEL_ORDERS', 'RECORD_PAYMENTS', 'REFUND_PAYMENTS', 'REQUEST_RETURNS', 'MANAGE_RETURNS', 'MANAGE_DOCUMENTS', 'VIEW_REPORTS'],
  enabled: true,
  disabledAt: null,
  disabledBy: null,
  disabledReason: null
};

const customer: UserAccount = {
  ...staff,
  id: 20,
  username: 'cliente',
  role: 'CUSTOMER',
  roleLabel: 'Cliente',
  permissions: ['VIEW_ORDERS', 'CREATE_ORDERS', 'CONFIRM_ORDERS', 'CANCEL_ORDERS', 'REQUEST_RETURNS']
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

describe('useOrderFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.fetchOrderPage.mockResolvedValue(pageOf([order]));
    apiMocks.fetchPartnerPage.mockResolvedValue(pageOf([partner], 6));
    apiMocks.fetchFinancialReconciliation.mockResolvedValue(financialReconciliation);
    apiMocks.fetchOrderDetail.mockResolvedValue({ order, documents: { canCreateInvoice: false, canCreateCreditNote: false, hasInvoice: false, hasCreditNote: false } });
    apiMocks.createOrder.mockResolvedValue(order);
    apiMocks.confirmOrder.mockResolvedValue({ ...order, status: 'CONFIRMED', statusLabel: 'Confermato' });
    apiMocks.fulfillOrder.mockResolvedValue({ ...order, status: 'FULFILLED', statusLabel: 'Evaso' });
    apiMocks.cancelOrder.mockResolvedValue({ ...order, status: 'CANCELED', statusLabel: 'Annullato' });
    apiMocks.recordOrderReceipt.mockResolvedValue(order);
    apiMocks.requestOrderReturn.mockResolvedValue(order);
    apiMocks.approveOrderReturn.mockResolvedValue(order);
    apiMocks.rejectOrderReturn.mockResolvedValue(order);
    apiMocks.receiveOrderReturn.mockResolvedValue(order);
    apiMocks.refundOrderReturn.mockResolvedValue(order);
  });

  it('carica ordini, clienti vendita e riconciliazione per lo staff', async () => {
    const { result } = renderOrderHook(staff);

    await waitFor(() => expect(result.current.orderPage.content).toEqual([order]));
    await waitFor(() => expect(result.current.salesCustomerPage.content).toEqual([partner]));
    expect(result.current.financialReconciliation).toEqual(financialReconciliation);
    expect(apiMocks.fetchOrderPage.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchPartnerPage.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchFinancialReconciliation.mock.calls[0][0]).toBeInstanceOf(AbortSignal);
  });

  it('isola il cliente da anagrafiche vendita e riconciliazione globale', async () => {
    const { result } = renderOrderHook(customer);

    await waitFor(() => expect(result.current.orderPage.content).toEqual([order]));
    expect(apiMocks.fetchPartnerPage).not.toHaveBeenCalled();
    expect(apiMocks.fetchFinancialReconciliation).not.toHaveBeenCalled();
  });

  it.each([
    ['401', Object.assign(new Error('Sessione scaduta'), { status: 401 })],
    ['403', Object.assign(new Error('Permesso negato'), { status: 403 })],
    ['500', Object.assign(new Error('Errore server'), { status: 500 })],
    ['offline', new TypeError('Failed to fetch')]
  ])('contiene l errore %s nello stato ordini', async (_, failure) => {
    apiMocks.fetchOrderPage.mockRejectedValue(failure);
    const { result } = renderOrderHook(customer);

    await waitFor(() => expect(result.current.ordersError).toBe(failure));
    expect(result.current.ordersLoading).toBe(false);
  });

  it('mantiene la risposta piu recente quando due pagine terminano fuori ordine', async () => {
    const first = deferred<PageResponse<Order>>();
    const second = deferred<PageResponse<Order>>();
    const newerOrder = { ...order, id: 2, code: 'ORD-0002' };
    apiMocks.fetchOrderPage.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const { result } = renderOrderHook(customer);
    await waitFor(() => expect(apiMocks.fetchOrderPage).toHaveBeenCalledOnce());

    act(() => result.current.setOrderQuery({ page: 1, size: 8 }));
    await waitFor(() => expect(apiMocks.fetchOrderPage).toHaveBeenCalledTimes(2));
    expect(apiMocks.fetchOrderPage.mock.calls[0][1].aborted).toBe(true);

    await act(async () => second.resolve(pageOf([newerOrder])));
    await waitFor(() => expect(result.current.orderPage.content[0].code).toBe('ORD-0002'));
    await act(async () => first.resolve(pageOf([order])));
    expect(result.current.orderPage.content[0].code).toBe('ORD-0002');
  });

  it('mantiene il dettaglio selezionato quando filtri e paginazione lo escludono dalla lista', async () => {
    apiMocks.fetchOrderPage
      .mockResolvedValueOnce(pageOf([order]))
      .mockResolvedValue(pageOf([]));
    const { result } = renderOrderHook(staff);
    await waitFor(() => expect(result.current.orderPage.content).toEqual([order]));

    act(() => result.current.selectOrder(order.code));
    await waitFor(() => expect(result.current.selectedOrderDetail?.order.code).toBe(order.code));
    expect(apiMocks.fetchOrderDetail).toHaveBeenCalledWith(order.code, expect.any(AbortSignal));

    act(() => result.current.setOrderQuery({ page: 1, size: 8, q: 'ordine non presente' }));
    await waitFor(() => expect(result.current.orderPage.content).toEqual([]));

    expect(result.current.selectedOrderCode).toBe(order.code);
    expect(result.current.selectedOrderDetail?.order.code).toBe(order.code);
    expect(result.current.selectedOrderDetail?.order.capabilities.canConfirm).toBe(true);
  });

  it('richiede un cliente censito prima della vendita assistita', async () => {
    const { result, callbacks } = renderOrderHook(staff);
    await waitFor(() => expect(result.current.orderPage.content).toHaveLength(1));
    act(() => result.current.addToCart(product));

    let saved = true;
    await act(async () => {
      saved = await result.current.checkout();
    });

    expect(saved).toBe(false);
    expect(apiMocks.createOrder).not.toHaveBeenCalled();
    expect(callbacks.onNotice).toHaveBeenCalledWith('Seleziona un cliente censito prima di creare la bozza.', 'error');
    expect(result.current.cart).toHaveLength(1);
  });

  it('crea una bozza con partner stabile e pulisce il carrello solo dopo conferma', async () => {
    const { result, callbacks } = renderOrderHook(staff);
    await waitFor(() => expect(result.current.salesCustomerPage.content).toEqual([partner]));
    act(() => {
      result.current.setSelectedSalesPartner(partner);
      result.current.addToCart(product);
    });

    let saved = false;
    await act(async () => {
      saved = await result.current.checkout();
    });

    expect(saved).toBe(true);
    expect(apiMocks.createOrder).toHaveBeenCalledWith({
      customerType: 'REGISTERED',
      customerPartnerId: partner.id,
      paymentMethod: 'CARD',
      items: [{ productCode: product.code, quantity: 1 }]
    });
    expect(result.current.cart).toEqual([]);
    expect(callbacks.onOpenOrders).toHaveBeenCalledOnce();
  });

  it('preserva la bozza quando la creazione ordine fallisce', async () => {
    apiMocks.createOrder.mockRejectedValue(new Error('offline'));
    const { result } = renderOrderHook(staff);
    act(() => {
      result.current.setSelectedSalesPartner(partner);
      result.current.addToCart(product);
    });

    let saved = true;
    await act(async () => {
      saved = await result.current.checkout();
    });

    expect(saved).toBe(false);
    expect(result.current.cart).toHaveLength(1);
    expect(result.current.selectedSalesPartner).toEqual(partner);
  });

  it('applica localmente una transizione ordine confermata', async () => {
    const { result } = renderOrderHook(staff);
    await waitFor(() => expect(result.current.orderPage.content[0].status).toBe('DRAFT'));

    await act(async () => result.current.confirm(order.code));

    expect(result.current.orderPage.content[0].status).toBe('CONFIRMED');
  });

  it('delega pagamenti, annullo e ciclo reso ai comandi di dominio corretti', async () => {
    const { result } = renderOrderHook(staff);
    await waitFor(() => expect(result.current.orderPage.content).toHaveLength(1));

    await act(async () => {
      await result.current.fulfill(order.code);
      await result.current.cancel(order.code, { reference: 'STORNO-1', reason: 'Duplicato' });
      await result.current.recordReceipt(order.code, { amount: 100, reference: 'POS-1', reason: 'Acconto' });
      await result.current.requestReturn(order.code, { reason: 'Difetto', items: [{ productCode: product.code, quantity: 1 }] });
      await result.current.approveReturn(order.code, 'RES-0001', 'Autorizzato');
      await result.current.receiveReturn(order.code, 'RES-0001');
      await result.current.refundReturn(order.code, 'RES-0001', { amount: 100, reference: 'RIM-1', reason: 'Rimborso' });
    });

    expect(apiMocks.fulfillOrder).toHaveBeenCalledWith(order.code);
    expect(apiMocks.cancelOrder).toHaveBeenCalledWith(order.code, { reference: 'STORNO-1', reason: 'Duplicato' });
    expect(apiMocks.recordOrderReceipt).toHaveBeenCalledWith(order.code, { amount: 100, reference: 'POS-1', reason: 'Acconto' });
    expect(apiMocks.requestOrderReturn).toHaveBeenCalledOnce();
    expect(apiMocks.approveOrderReturn).toHaveBeenCalledWith(order.code, 'RES-0001', 'Autorizzato');
    expect(apiMocks.receiveOrderReturn).toHaveBeenCalledWith(order.code, 'RES-0001');
    expect(apiMocks.refundOrderReturn).toHaveBeenCalledOnce();
  });

  it('limita conferma e annullo del cliente alle proprie bozze', async () => {
    const { result } = renderOrderHook(customer);
    const ownDraft = { ...order, customerType: 'SELF_SERVICE' as const, ownershipStatus: 'ACCOUNT' as const, customerAccountId: customer.id, partnerId: undefined };
    const otherDraft = { ...ownDraft, customerAccountId: 999, capabilities: { ...ownDraft.capabilities, canConfirm: false, canCancel: false } };

    expect(result.current.canConfirm(ownDraft)).toBe(true);
    expect(result.current.canCancel(ownDraft)).toBe(true);
    expect(result.current.canConfirm(otherDraft)).toBe(false);
    expect(result.current.canCancel(otherDraft)).toBe(false);
    expect(result.current.canFulfill({ ...ownDraft, status: 'CONFIRMED', capabilities: { ...ownDraft.capabilities, canFulfill: false } })).toBe(false);
  });

  it('rifiuta un reso privo di motivazione senza inviare il comando', async () => {
    const { result, callbacks } = renderOrderHook(staff);

    let outcome: CommandOutcome = { status: 'success', saved: true };
    await act(async () => {
      outcome = await result.current.rejectReturn(order.code, 'RES-0001', '  ');
    });

    expect(outcome).toEqual({ status: 'error', saved: false });
    expect(apiMocks.rejectOrderReturn).not.toHaveBeenCalled();
    expect(callbacks.onNotice).toHaveBeenCalledWith('Inserisci una motivazione prima di rifiutare il reso.', 'error');
  });

  it('limita il carrello alla disponibilita interna e blocca prodotti disattivati', async () => {
    const { result, callbacks } = renderOrderHook(staff);
    const limitedProduct: Product = { ...product, availableQuantity: 1 };
    const discontinuedProduct: Product = { ...product, code: 'GPU-OLD', discontinued: true };
    act(() => {
      result.current.addToCart(limitedProduct);
      result.current.addToCart(limitedProduct);
      result.current.addToCart(discontinuedProduct);
    });

    expect(result.current.cart).toEqual([{ product: limitedProduct, quantity: 1, maximumQuantity: 1 }]);
    expect(callbacks.onNotice).toHaveBeenCalledWith('Prodotto disattivato e non disponibile per nuovi ordini.', 'error');
  });
});

function renderOrderHook(currentUser: UserAccount) {
  const callbacks = {
    onRefreshDashboard: vi.fn().mockResolvedValue(undefined),
    onRefreshInventory: vi.fn().mockResolvedValue(undefined),
    onOpenOrders: vi.fn(),
    onNotice: vi.fn()
  };
  const rendered = renderHook(() => useOrderFlow({
    enabled: true,
    currentUser,
    executeCommand: executorWithoutRefresh,
    ...callbacks
  }));
  return { ...rendered, callbacks };
}

function pageOf<T>(content: T[], size = 8): PageResponse<T> {
  return {
    content,
    page: 0,
    size,
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
