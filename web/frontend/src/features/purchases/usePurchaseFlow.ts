import { useEffect, useMemo, useState } from 'react';
import {
  BusinessPartner,
  cancelSupplierOrder,
  createSupplierOrder,
  CreateSupplierOrderPayload,
  fetchPartnerPage,
  fetchSupplierOrder,
  fetchSupplierOrderPage,
  PageResponse,
  receiveSupplierOrder,
  ReceiveSupplierOrderPayload,
  sendSupplierOrder,
  SupplierOrder,
  SupplierOrderQuery,
  SupplierOrderSummary
} from '../../api';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import useDraftState from '../../hooks/useDraftState';
import { type CommandExecutor } from '../../hooks/useCommandExecution';
import usePaginatedResource from '../../hooks/usePaginatedResource';

const PAGE_SIZE = 8;
const INITIAL_QUERY: SupplierOrderQuery = { page: 0, size: PAGE_SIZE };

export type SupplierOrderDraftLine = {
  productCode: string;
  quantity: string;
  unitPrice: string;
  expectedDeliveryDate: string;
};

export type SupplierOrderDraft = {
  supplierId: string;
  expectedDeliveryDate: string;
  notes: string;
  items: SupplierOrderDraftLine[];
};

type Options = {
  enabled: boolean;
  accountId?: number;
  executeCommand: CommandExecutor;
  onInventoryChanged: () => Promise<void>;
};

export default function usePurchaseFlow({ enabled, accountId, executeCommand, onInventoryChanged }: Options) {
  const [query, setQuery] = useState<SupplierOrderQuery>(INITIAL_QUERY);
  const [selectedCode, setSelectedCode] = useState<string | null>(null);
  const [detail, setDetail] = useState<SupplierOrder | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<unknown>(null);
  const [suppliers, setSuppliers] = useState<BusinessPartner[]>([]);
  const [supplierError, setSupplierError] = useState<unknown>(null);
  const initialDraft = useMemo(createEmptyDraft, []);
  const { value: draft, setValue: setDraft, dirty: draftDirty, clear: clearDraft } = useDraftState({
    key: `supplier-order:${accountId ?? 'anonymous'}`,
    view: 'purchases',
    label: 'Nuovo ordine fornitore',
    initialValue: initialDraft,
    enabled
  });
  const debouncedSearch = useDebouncedValue(query.q ?? '', 300);
  const requestQuery = useMemo(() => ({ ...query, q: debouncedSearch }), [debouncedSearch, query]);
  const emptyOrders = useMemo(() => emptyPage<SupplierOrderSummary>(), []);
  const orders = usePaginatedResource({ query: requestQuery, enabled, initialData: emptyOrders, loader: fetchSupplierOrderPage });

  useEffect(() => {
    if (!enabled) {
      setSuppliers([]);
      setSupplierError(null);
      return;
    }
    const controller = new AbortController();
    fetchPartnerPage({ type: 'SUPPLIER', active: true, page: 0, size: 200 }, controller.signal)
      .then((page) => setSuppliers(page.content))
      .catch((error) => {
        if (!(error instanceof DOMException && error.name === 'AbortError')) setSupplierError(error);
      });
    return () => controller.abort();
  }, [enabled]);

  useEffect(() => {
    if (!enabled || !selectedCode) {
      setDetail(null);
      setDetailError(null);
      return;
    }
    const controller = new AbortController();
    setDetailLoading(true);
    setDetailError(null);
    fetchSupplierOrder(selectedCode, controller.signal)
      .then(setDetail)
      .catch((error) => {
        if (!(error instanceof DOMException && error.name === 'AbortError')) setDetailError(error);
      })
      .finally(() => {
        if (!controller.signal.aborted) setDetailLoading(false);
      });
    return () => controller.abort();
  }, [enabled, selectedCode]);

  async function submitDraft(): Promise<void> {
    const payload = toPayload(draft);
    await executeCommand({
      key: `supplier-order:create:${payload.supplierId}`,
      command: () => createSupplierOrder(payload),
      applyResponse: (order) => {
        setDetail(order);
        setSelectedCode(order.code);
      },
      afterConfirmed: clearDraft,
      refresh: refreshOrders,
      successMessage: 'Ordine fornitore creato in bozza.'
    });
  }

  async function send(code: string) {
    await executeMutation(`supplier-order:send:${code}`, () => sendSupplierOrder(code), 'Ordine inviato al fornitore.');
  }

  async function receive(code: string, payload: ReceiveSupplierOrderPayload) {
    await executeMutation(
      `supplier-order:receive:${code}`,
      () => receiveSupplierOrder(code, payload),
      'Ricezione registrata: giacenza, ledger e costo prodotto sono stati aggiornati.',
      onInventoryChanged
    );
  }

  async function cancel(code: string, reason: string) {
    await executeMutation(`supplier-order:cancel:${code}`, () => cancelSupplierOrder(code, reason), 'Ordine annullato; quantita gia ricevute e storico sono stati preservati.');
  }

  async function executeMutation(key: string, command: () => Promise<SupplierOrder>, successMessage: string, afterConfirmed?: () => Promise<void>) {
    await executeCommand({
      key,
      command,
      applyResponse: setDetail,
      refresh: refreshOrders,
      afterConfirmed,
      successMessage
    });
  }

  async function refreshOrders() {
    await orders.refresh();
  }

  async function refreshDetail() {
    if (!selectedCode) return;
    setDetail(await fetchSupplierOrder(selectedCode));
  }

  async function refreshResources() {
    await Promise.all([refreshOrders(), refreshDetail()]);
  }

  function reset() {
    setQuery(INITIAL_QUERY);
    setSelectedCode(null);
    setDetail(null);
    setDetailError(null);
    setSuppliers([]);
    setSupplierError(null);
    orders.setData(emptyOrders);
    clearDraft();
  }

  return {
    query,
    page: orders.data,
    loading: orders.loading,
    refreshing: orders.refreshing,
    error: orders.error,
    pageSize: PAGE_SIZE,
    selectedCode,
    detail,
    detailLoading,
    detailError,
    suppliers,
    supplierError,
    draft,
    draftDirty,
    setQuery,
    setSelectedCode,
    setDraft,
    clearDraft,
    submitDraft,
    send,
    receive,
    cancel,
    refreshResources,
    reset
  };
}

export type PurchaseFlowController = ReturnType<typeof usePurchaseFlow>;

function createEmptyDraft(): SupplierOrderDraft {
  const expected = new Date();
  expected.setDate(expected.getDate() + 7);
  return {
    supplierId: '',
    expectedDeliveryDate: expected.toISOString().slice(0, 10),
    notes: '',
    items: [{ productCode: '', quantity: '1', unitPrice: '0.00', expectedDeliveryDate: '' }]
  };
}

function toPayload(draft: SupplierOrderDraft): CreateSupplierOrderPayload {
  return {
    supplierId: Number(draft.supplierId),
    expectedDeliveryDate: draft.expectedDeliveryDate,
    notes: draft.notes,
    items: draft.items.map((item) => ({
      productCode: item.productCode,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      ...(item.expectedDeliveryDate ? { expectedDeliveryDate: item.expectedDeliveryDate } : {})
    }))
  };
}

function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0, first: true, last: true };
}
