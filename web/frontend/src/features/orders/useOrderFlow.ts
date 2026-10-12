import { useMemo, useState } from 'react';
import {
  approveOrderReturn,
  BusinessPartner,
  CancellationPayload,
  cancelOrder,
  confirmOrder,
  createOrder,
  CustomerProduct,
  fetchFinancialReconciliation,
  fetchOrderDetail,
  fetchOrderPage,
  fetchPartnerPage,
  FinancialReconciliation,
  fulfillOrder,
  Order,
  OrderOperationalDetail,
  OrderQuery,
  PageResponse,
  PartnerQuery,
  PaymentMethod,
  Product,
  ReceiptPayload,
  receiveOrderReturn,
  recordOrderReceipt,
  refundOrderReturn,
  rejectOrderReturn,
  requestOrderReturn,
  ReturnRefundPayload,
  ReturnRequestPayload,
  SellableProduct,
  UserAccount,
  UserPermission
} from '../../api';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import useDraftState from '../../hooks/useDraftState';
import { commandOutcome, commandWasSaved, type CommandExecutor, type CommandOutcome, type UiNotice } from '../../hooks/useCommandExecution';
import usePaginatedResource from '../../hooks/usePaginatedResource';
import { CartItem, SalesCustomerMode } from '../../types/ui';
import { upsertPageItem } from '../../utils/pageState';

const DEFAULT_PAGE_SIZE = 8;
const SALES_CUSTOMER_PAGE_SIZE = 6;
const INITIAL_ORDER_QUERY: OrderQuery = { page: 0, size: DEFAULT_PAGE_SIZE };
const INITIAL_CUSTOMER_QUERY: PartnerQuery = { page: 0, size: SALES_CUSTOMER_PAGE_SIZE, type: 'CUSTOMER', active: true };
const FINANCIAL_QUERY = { scope: 'current' };
const INITIAL_SALES_DRAFT = {
  cart: [] as CartItem[],
  paymentMethod: 'CARD' as PaymentMethod,
  salesCustomerMode: 'REGISTERED' as SalesCustomerMode,
  selectedSalesPartner: undefined as BusinessPartner | undefined,
  walkInCustomerName: ''
};

type OrderFlowOptions = {
  enabled: boolean;
  currentUser: UserAccount | null;
  executeCommand: CommandExecutor;
  onRefreshDashboard: () => Promise<void>;
  onRefreshInventory: () => Promise<void>;
  onOpenOrders: () => void;
  onNotice: (message: string, tone?: UiNotice['tone'], title?: string) => void;
};

export default function useOrderFlow({
  enabled,
  currentUser,
  executeCommand,
  onRefreshDashboard,
  onRefreshInventory,
  onOpenOrders,
  onNotice
}: OrderFlowOptions) {
  const [orderQuery, setOrderQuery] = useState<OrderQuery>(INITIAL_ORDER_QUERY);
  const [salesCustomerQuery, setSalesCustomerQuery] = useState<PartnerQuery>(INITIAL_CUSTOMER_QUERY);
  const { value: salesDraft, setValue: setSalesDraft, dirty: salesDraftDirty, clear: clearSalesDraft } = useDraftState({
    key: `sales:${currentUser?.id ?? 'anonymous'}`,
    view: 'sales',
    label: 'Bozza vendita',
    initialValue: INITIAL_SALES_DRAFT,
    enabled: enabled && hasPermission(currentUser, 'CREATE_ORDERS')
  });
  const { cart, paymentMethod, salesCustomerMode, selectedSalesPartner, walkInCustomerName } = salesDraft;
  const [selectedOrderCode, setSelectedOrderCode] = useState('');
  const canCreateOrders = hasPermission(currentUser, 'CREATE_ORDERS');
  const canViewReports = hasPermission(currentUser, 'VIEW_REPORTS');
  const canManageDocuments = hasPermission(currentUser, 'MANAGE_DOCUMENTS');
  const canRecordPayments = hasPermission(currentUser, 'RECORD_PAYMENTS');
  const canRefundPayments = hasPermission(currentUser, 'REFUND_PAYMENTS');
  const canRequestReturns = hasPermission(currentUser, 'REQUEST_RETURNS');
  const canManageReturns = hasPermission(currentUser, 'MANAGE_RETURNS');

  const debouncedOrderSearch = useDebouncedValue(orderQuery.q ?? '', 300);
  const debouncedOrderCustomer = useDebouncedValue(orderQuery.customer ?? '', 300);
  const debouncedSalesCustomerSearch = useDebouncedValue(salesCustomerQuery.q ?? '', 300);
  const orderRequestQuery = useMemo<OrderQuery>(() => ({
    ...orderQuery,
    q: debouncedOrderSearch,
    customer: debouncedOrderCustomer
  }), [debouncedOrderCustomer, debouncedOrderSearch, orderQuery]);
  const salesCustomerRequestQuery = useMemo<PartnerQuery>(() => ({
    ...salesCustomerQuery,
    q: debouncedSalesCustomerSearch,
    type: 'CUSTOMER',
    active: true
  }), [debouncedSalesCustomerSearch, salesCustomerQuery]);
  const emptyOrders = useMemo(() => emptyPage<Order>(DEFAULT_PAGE_SIZE), []);
  const emptyCustomers = useMemo(() => emptyPage<BusinessPartner>(SALES_CUSTOMER_PAGE_SIZE), []);

  const orders = usePaginatedResource({
    query: orderRequestQuery,
    enabled,
    initialData: emptyOrders,
    loader: fetchOrderPage
  });
  const salesCustomers = usePaginatedResource({
    query: salesCustomerRequestQuery,
    enabled: enabled && currentUser?.role !== 'CUSTOMER' && canCreateOrders,
    initialData: emptyCustomers,
    loader: fetchPartnerPage
  });
  const financialReconciliation = usePaginatedResource({
    query: FINANCIAL_QUERY,
    enabled: enabled && canViewReports,
    initialData: null as FinancialReconciliation | null,
    loader: loadFinancialReconciliation
  });
  const orderDetail = usePaginatedResource({
    query: { code: selectedOrderCode },
    enabled: enabled && Boolean(selectedOrderCode),
    initialData: null as OrderOperationalDetail | null,
    loader: loadOrderDetail
  });

  function applyOrder(order: Order) {
    orders.setData((page) => upsertPageItem(page, order, (item) => item.code));
    orderDetail.setData((detail) => detail?.order.code === order.code ? { ...detail, order } : detail);
  }

  async function refreshOrderData(includeInventory = false) {
    const requests: Promise<unknown>[] = [orders.refresh(), onRefreshDashboard()];
    if (canViewReports) requests.push(financialReconciliation.refresh());
    if (includeInventory) requests.push(onRefreshInventory());
    if (selectedOrderCode) requests.push(orderDetail.refresh());
    await Promise.all(requests);
  }

  async function refreshResources() {
    const requests: Promise<unknown>[] = [orders.refresh()];
    if (currentUser?.role !== 'CUSTOMER' && canCreateOrders) requests.push(salesCustomers.refresh());
    if (canViewReports) requests.push(financialReconciliation.refresh());
    if (selectedOrderCode) requests.push(orderDetail.refresh());
    await Promise.all(requests);
  }

  function addToCart(product: SellableProduct) {
    if (isInternalProduct(product) && product.discontinued) {
      onNotice('Prodotto disattivato e non disponibile per nuovi ordini.', 'error');
      return;
    }
    if ((isInternalProduct(product) && product.availableQuantity <= 0)
      || (isCustomerProduct(product) && product.availability === 'UNAVAILABLE')) {
      onNotice('Prodotto non disponibile alla vendita.', 'error');
      return;
    }
    const maximumQuantity = isInternalProduct(product) ? product.availableQuantity : undefined;
    setSalesDraft((current) => {
      const existing = current.cart.find((item) => item.product.code === product.code);
      if (existing) {
        if (maximumQuantity !== undefined && existing.quantity >= maximumQuantity) {
          onNotice('Disponibilita vendibile massima gia selezionata.', 'info');
          return current;
        }
        return { ...current, cart: current.cart.map((item) => item.product.code === product.code ? { ...item, quantity: item.quantity + 1 } : item) };
      }
      return { ...current, cart: [...current.cart, { product, quantity: 1, maximumQuantity }] };
    });
  }

  function changeCartQuantity(productCode: string, quantity: number) {
    setSalesDraft((current) => ({ ...current, cart: current.cart.flatMap((item) => {
      if (item.product.code !== productCode) return [item];
      if (quantity <= 0) return [];
      return [{ ...item, quantity: item.maximumQuantity === undefined ? quantity : Math.min(quantity, item.maximumQuantity) }];
    }) }));
  }

  async function checkout(): Promise<boolean> {
    if (!currentUser || cart.length === 0) return false;
    if (currentUser.role !== 'CUSTOMER' && salesCustomerMode === 'REGISTERED' && !selectedSalesPartner) {
      onNotice('Seleziona un cliente censito prima di creare la bozza.', 'error');
      return false;
    }
    if (currentUser.role !== 'CUSTOMER' && salesCustomerMode === 'WALK_IN' && !walkInCustomerName.trim()) {
      onNotice('Inserisci il nominativo del cliente occasionale.', 'error');
      return false;
    }
    const submittedCart = [...cart];
    const submittedPaymentMethod = paymentMethod;
    const submittedPartner = selectedSalesPartner;
    const submittedWalkInName = walkInCustomerName.trim();
    const customerSelection = currentUser.role === 'CUSTOMER'
      ? { customerType: 'SELF_SERVICE' as const }
      : salesCustomerMode === 'REGISTERED'
        ? { customerType: 'REGISTERED' as const, customerPartnerId: submittedPartner!.id }
        : { customerType: 'WALK_IN' as const, walkInCustomerName: submittedWalkInName };
    const result = await executeCommand({
      key: 'order:create',
      command: () => createOrder({
        ...customerSelection,
        paymentMethod: submittedPaymentMethod,
        items: submittedCart.map((item) => ({ productCode: item.product.code, quantity: item.quantity }))
      }),
      applyResponse: applyOrder,
      afterConfirmed: () => {
        clearDraft();
        onOpenOrders();
      },
      refresh: () => refreshOrderData(),
      successMessage: 'Ordine creato in bozza. Confermalo dalla sezione Ordini per impegnare il magazzino.'
    });
    return commandWasSaved(result);
  }

  async function confirm(code: string) {
    await executeCommand({
      key: `order:confirm:${code}`,
      command: () => confirmOrder(code),
      applyResponse: applyOrder,
      refresh: () => refreshOrderData(true),
      successMessage: `Ordine ${code} confermato.`
    });
  }

  async function fulfill(code: string) {
    await executeCommand({
      key: `order:fulfill:${code}`,
      command: () => fulfillOrder(code),
      applyResponse: applyOrder,
      refresh: () => refreshOrderData(true),
      successMessage: `Ordine ${code} evaso.`
    });
  }

  async function cancel(code: string, payload: CancellationPayload) {
    await executeCommand({
      key: `order:cancel:${code}`,
      command: () => cancelOrder(code, payload),
      applyResponse: applyOrder,
      refresh: () => refreshOrderData(true),
      successMessage: `Ordine ${code} annullato.`
    });
  }

  async function recordReceipt(code: string, payload: ReceiptPayload) {
    await executeCommand({
      key: `order:receipt:${code}`,
      command: () => recordOrderReceipt(code, payload),
      applyResponse: applyOrder,
      refresh: () => refreshOrderData(),
      successMessage: `Incasso registrato sull'ordine ${code}.`
    });
  }

  async function requestReturn(code: string, payload: ReturnRequestPayload): Promise<CommandOutcome> {
    const result = await executeCommand({
      key: `return:create:${code}`,
      command: () => requestOrderReturn(code, payload),
      applyResponse: applyOrder,
      refresh: () => orders.refresh().then(() => undefined),
      successMessage: `Richiesta di reso registrata per l'ordine ${code}.`
    });
    return commandOutcome(result);
  }

  async function approveReturn(orderCode: string, returnCode: string, note: string): Promise<CommandOutcome> {
    const result = await executeCommand({
      key: `return:approve:${returnCode}`,
      command: () => approveOrderReturn(orderCode, returnCode, note),
      applyResponse: applyOrder,
      refresh: () => orders.refresh().then(() => undefined),
      successMessage: `Reso ${returnCode} approvato.`
    });
    return commandOutcome(result);
  }

  async function rejectReturn(orderCode: string, returnCode: string, note: string): Promise<CommandOutcome> {
    if (!note.trim()) {
      onNotice('Inserisci una motivazione prima di rifiutare il reso.', 'error');
      return { status: 'error', saved: false };
    }
    const result = await executeCommand({
      key: `return:reject:${returnCode}`,
      command: () => rejectOrderReturn(orderCode, returnCode, note),
      applyResponse: applyOrder,
      refresh: () => orders.refresh().then(() => undefined),
      successMessage: `Reso ${returnCode} rifiutato.`
    });
    return commandOutcome(result);
  }

  async function receiveReturn(orderCode: string, returnCode: string): Promise<CommandOutcome> {
    const result = await executeCommand({
      key: `return:receive:${returnCode}`,
      command: () => receiveOrderReturn(orderCode, returnCode),
      applyResponse: applyOrder,
      refresh: () => refreshOrderData(true),
      successMessage: `Reso ${returnCode} ricevuto e magazzino aggiornato.`
    });
    return commandOutcome(result);
  }

  async function refundReturn(orderCode: string, returnCode: string, payload: ReturnRefundPayload): Promise<CommandOutcome> {
    const result = await executeCommand({
      key: `return:refund:${returnCode}`,
      command: () => refundOrderReturn(orderCode, returnCode, payload),
      applyResponse: applyOrder,
      refresh: () => refreshOrderData(),
      successMessage: `Rimborso registrato sul reso ${returnCode}.`
    });
    return commandOutcome(result);
  }

  function canConfirm(order: Order) {
    return order.capabilities.canConfirm;
  }

  function canFulfill(order: Order) {
    return order.capabilities.canFulfill;
  }

  function canCancel(order: Order) {
    return order.capabilities.canCancel;
  }

  function changeCustomerMode(mode: SalesCustomerMode) {
    setSalesDraft((current) => ({ ...current, salesCustomerMode: mode, selectedSalesPartner: undefined, walkInCustomerName: '' }));
  }

  function clearDraft() {
    clearSalesDraft();
  }

  function reset() {
    setOrderQuery(INITIAL_ORDER_QUERY);
    setSalesCustomerQuery(INITIAL_CUSTOMER_QUERY);
    clearDraft();
    orders.setData(emptyOrders);
    salesCustomers.setData(emptyCustomers);
    financialReconciliation.setData(null);
    setSelectedOrderCode('');
    orderDetail.setData(null);
  }

  return {
    orderQuery,
    orderPage: orders.data,
    salesCustomerQuery,
    salesCustomerPage: salesCustomers.data,
    financialReconciliation: financialReconciliation.data,
    cart,
    salesDraftDirty,
    paymentMethod,
    salesCustomerMode,
    selectedSalesPartner,
    walkInCustomerName,
    selectedOrderCode,
    selectedOrderDetail: orderDetail.data,
    orderDetailLoading: orderDetail.loading,
    orderDetailRefreshing: orderDetail.refreshing,
    orderDetailError: orderDetail.error,
    canCreateOrders,
    canViewReports,
    canManageDocuments,
    canRecordPayments,
    canRefundPayments,
    canRequestReturns,
    canManageReturns,
    ordersLoading: orders.loading || financialReconciliation.loading,
    ordersRefreshing: orders.refreshing || financialReconciliation.refreshing,
    ordersError: orders.error ?? financialReconciliation.error,
    salesCustomersLoading: salesCustomers.loading,
    salesCustomersRefreshing: salesCustomers.refreshing,
    salesCustomersError: salesCustomers.error,
    setOrderQuery,
    setSalesCustomerQuery,
    setPaymentMethod: (value: PaymentMethod) => setSalesDraft((current) => ({ ...current, paymentMethod: value })),
    setSelectedSalesPartner: (value: BusinessPartner | undefined) => setSalesDraft((current) => ({ ...current, selectedSalesPartner: value })),
    setWalkInCustomerName: (value: string) => setSalesDraft((current) => ({ ...current, walkInCustomerName: value })),
    selectOrder: setSelectedOrderCode,
    changeCustomerMode,
    addToCart,
    changeCartQuantity,
    clearDraft,
    checkout,
    confirm,
    fulfill,
    cancel,
    recordReceipt,
    requestReturn,
    approveReturn,
    rejectReturn,
    receiveReturn,
    refundReturn,
    canConfirm,
    canFulfill,
    canCancel,
    applyOrder,
    refreshOrderData,
    refreshResources,
    reset
  };
}

export type OrderFlowController = ReturnType<typeof useOrderFlow>;

function emptyPage<T>(size: number): PageResponse<T> {
  return { content: [], page: 0, size, totalElements: 0, totalPages: 0, first: true, last: true };
}

function loadFinancialReconciliation(_: typeof FINANCIAL_QUERY, signal: AbortSignal) {
  return fetchFinancialReconciliation(signal);
}

function loadOrderDetail(query: { code: string }, signal: AbortSignal) {
  return fetchOrderDetail(query.code, signal);
}

function hasPermission(user: UserAccount | null, permission: UserPermission) {
  return Boolean(user?.permissions.includes(permission));
}

function isInternalProduct(product: SellableProduct): product is Product {
  return 'availableQuantity' in product && 'discontinued' in product;
}

function isCustomerProduct(product: SellableProduct): product is CustomerProduct {
  return 'availability' in product;
}
