import { useEffect, useMemo, useRef, useState } from 'react';
import {
  createCreditNote,
  createInvoice,
  DocumentQuery,
  downloadInventoryReport,
  downloadSalesReport,
  fetchDocumentPage,
  fetchInventoryReport,
  fetchSalesReport,
  FiscalDocument,
  InventoryReport,
  InventoryReportQuery,
  PageResponse,
  ReportFormat,
  SalesReport,
  SalesReportQuery
} from '../../api';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import type { CommandExecutor, NoticeTone } from '../../hooks/useCommandExecution';
import usePaginatedResource from '../../hooks/usePaginatedResource';
import { upsertPageItem } from '../../utils/pageState';

const DEFAULT_PAGE_SIZE = 8;
const INITIAL_DOCUMENT_QUERY: DocumentQuery = { page: 0, size: DEFAULT_PAGE_SIZE };
const INITIAL_SALES_QUERY: SalesReportQuery = { status: 'FULFILLED' };
const INITIAL_INVENTORY_QUERY: InventoryReportQuery = { stock: 'ALL', discontinued: false };
const INITIAL_CREDIT_REASON = 'Rettifica documento simulato';

type DocumentReportFlowOptions = {
  documentsEnabled: boolean;
  reportsEnabled: boolean;
  reportsActive: boolean;
  executeCommand: CommandExecutor;
  onOpenDocuments: () => void;
  onNotice: (message: string, tone?: NoticeTone, title?: string) => void;
  onError: (error: unknown) => void;
};

export default function useDocumentReportFlow({
  documentsEnabled,
  reportsEnabled,
  reportsActive,
  executeCommand,
  onOpenDocuments,
  onNotice,
  onError
}: DocumentReportFlowOptions) {
  const [documentQuery, setDocumentQuery] = useState<DocumentQuery>(INITIAL_DOCUMENT_QUERY);
  const [salesQuery, setSalesQuery] = useState<SalesReportQuery>(INITIAL_SALES_QUERY);
  const [inventoryQuery, setInventoryQuery] = useState<InventoryReportQuery>(INITIAL_INVENTORY_QUERY);
  const [creditReason, setCreditReason] = useState(INITIAL_CREDIT_REASON);
  const [reportsActivated, setReportsActivated] = useState(false);
  const [exporting, setExporting] = useState(false);
  const activeExports = useRef(new Set<string>());
  const debouncedDocumentSearch = useDebouncedValue(documentQuery.q ?? '', 300);
  const debouncedInventorySearch = useDebouncedValue(inventoryQuery.q ?? '', 300);
  const documentRequestQuery = useMemo<DocumentQuery>(() => ({
    ...documentQuery,
    q: debouncedDocumentSearch
  }), [debouncedDocumentSearch, documentQuery]);
  const inventoryRequestQuery = useMemo<InventoryReportQuery>(() => ({
    ...inventoryQuery,
    q: debouncedInventorySearch
  }), [debouncedInventorySearch, inventoryQuery]);
  const emptyDocuments = useMemo(() => emptyPage<FiscalDocument>(), []);

  useEffect(() => {
    if (!reportsEnabled) {
      setReportsActivated(false);
      return;
    }
    if (reportsActive) setReportsActivated(true);
  }, [reportsActive, reportsEnabled]);

  const documents = usePaginatedResource({
    query: documentRequestQuery,
    enabled: documentsEnabled,
    initialData: emptyDocuments,
    loader: fetchDocumentPage
  });
  const sales = usePaginatedResource({
    query: salesQuery,
    enabled: reportsEnabled && reportsActivated,
    initialData: null as SalesReport | null,
    loader: fetchSalesReport
  });
  const inventory = usePaginatedResource({
    query: inventoryRequestQuery,
    enabled: reportsEnabled && reportsActivated,
    initialData: null as InventoryReport | null,
    loader: fetchInventoryReport
  });

  function applyDocument(document: FiscalDocument) {
    documents.setData((page) => upsertPageItem(page, document, (item) => item.code));
  }

  async function createInvoiceForOrder(orderCode: string) {
    await executeCommand({
      key: `document:invoice:${orderCode}`,
      command: () => createInvoice(orderCode),
      applyResponse: applyDocument,
      afterConfirmed: onOpenDocuments,
      refresh: refreshDocuments,
      successMessage: `Fattura simulata registrata per l'ordine ${orderCode}.`
    });
  }

  async function createCreditNoteForOrder(orderCode: string) {
    const submittedReason = creditReason;
    await executeCommand({
      key: `document:credit-note:${orderCode}`,
      command: () => createCreditNote(orderCode, submittedReason),
      applyResponse: applyDocument,
      afterConfirmed: onOpenDocuments,
      refresh: refreshDocuments,
      successMessage: `Nota di credito simulata registrata per l'ordine ${orderCode}.`
    });
  }

  async function exportReport(kind: 'sales' | 'inventory', format: ReportFormat): Promise<boolean> {
    const key = `${kind}:${format}`;
    if (activeExports.current.has(key)) {
      onNotice('Il download richiesto e gia in preparazione.', 'info', 'Esportazione in corso');
      return false;
    }

    activeExports.current.add(key);
    setExporting(true);
    try {
      if (kind === 'sales') await downloadSalesReport(salesQuery, format);
      else await downloadInventoryReport(inventoryQuery, format);
      onNotice(`Report ${kind === 'sales' ? 'vendite' : 'magazzino'} esportato in formato ${format}.`, 'success', 'Esportazione completata');
      return true;
    } catch (error) {
      onError(error);
      return false;
    } finally {
      activeExports.current.delete(key);
      setExporting(activeExports.current.size > 0);
    }
  }

  async function refreshDocuments() {
    await documents.refresh();
  }

  async function refreshSales() {
    await sales.refresh();
  }

  async function refreshInventory() {
    await inventory.refresh();
  }

  async function refreshReports() {
    await Promise.all([refreshSales(), refreshInventory()]);
  }

  async function refreshResources() {
    await Promise.all([refreshDocuments(), refreshReports()]);
  }

  function reset() {
    setDocumentQuery(INITIAL_DOCUMENT_QUERY);
    setSalesQuery(INITIAL_SALES_QUERY);
    setInventoryQuery(INITIAL_INVENTORY_QUERY);
    setCreditReason(INITIAL_CREDIT_REASON);
    setReportsActivated(false);
    activeExports.current.clear();
    setExporting(false);
    documents.setData(emptyDocuments);
    sales.setData(null);
    inventory.setData(null);
  }

  return {
    documentQuery,
    documentPage: documents.data,
    creditReason,
    salesQuery,
    salesReport: sales.data,
    inventoryQuery,
    inventoryReport: inventory.data,
    documentLoading: documents.loading,
    documentRefreshing: documents.refreshing,
    documentError: documents.error,
    salesLoading: sales.loading,
    salesRefreshing: sales.refreshing,
    salesError: sales.error,
    inventoryLoading: inventory.loading,
    inventoryRefreshing: inventory.refreshing,
    inventoryError: inventory.error,
    exporting,
    pageSize: DEFAULT_PAGE_SIZE,
    setDocumentQuery,
    setCreditReason,
    setSalesQuery,
    setInventoryQuery,
    createInvoiceForOrder,
    createCreditNoteForOrder,
    exportReport,
    refreshDocuments,
    refreshSales,
    refreshInventory,
    refreshReports,
    refreshResources,
    reset
  };
}

export type DocumentReportFlowController = ReturnType<typeof useDocumentReportFlow>;

function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: DEFAULT_PAGE_SIZE, totalElements: 0, totalPages: 0, first: true, last: true };
}
