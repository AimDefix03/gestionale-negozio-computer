import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type {
  FiscalDocument,
  InventoryReport,
  PageResponse,
  SalesReport
} from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import useDocumentReportFlow from './useDocumentReportFlow';

const apiMocks = vi.hoisted(() => ({
  createCreditNote: vi.fn(),
  createInvoice: vi.fn(),
  downloadInventoryReport: vi.fn(),
  downloadSalesReport: vi.fn(),
  fetchDocumentPage: vi.fn(),
  fetchInventoryReport: vi.fn(),
  fetchSalesReport: vi.fn()
}));

vi.mock('../../api', () => apiMocks);

const invoice = documentFixture();
const salesReport = salesFixture();
const inventoryReport = inventoryFixture();

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

describe('useDocumentReportFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMocks.fetchDocumentPage.mockResolvedValue(pageOf([invoice]));
    apiMocks.fetchSalesReport.mockResolvedValue(salesReport);
    apiMocks.fetchInventoryReport.mockResolvedValue(inventoryReport);
    apiMocks.createInvoice.mockResolvedValue(invoice);
    apiMocks.createCreditNote.mockResolvedValue({ ...invoice, id: 2, code: 'NC-2026-0001', type: 'SIMULATED_CREDIT_NOTE', typeLabel: 'Nota di credito simulata' });
    apiMocks.downloadSalesReport.mockResolvedValue(undefined);
    apiMocks.downloadInventoryReport.mockResolvedValue(undefined);
  });

  it('carica documenti e report tramite richieste cancellabili', async () => {
    const { result } = renderDocumentHook();

    await waitFor(() => expect(result.current.documentPage.content).toEqual([invoice]));
    await waitFor(() => expect(result.current.salesReport).toEqual(salesReport));
    await waitFor(() => expect(result.current.inventoryReport).toEqual(inventoryReport));
    expect(apiMocks.fetchDocumentPage.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchSalesReport.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
    expect(apiMocks.fetchInventoryReport.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
  });

  it('attiva i report solo alla prima apertura e conserva i dati lasciando la vista', async () => {
    const { result, rerender } = renderHook(
      ({ active }) => useDocumentReportFlow(flowOptions({ reportsActive: active })),
      { initialProps: { active: false } }
    );

    await waitFor(() => expect(result.current.documentPage.content).toEqual([invoice]));
    expect(apiMocks.fetchSalesReport).not.toHaveBeenCalled();
    rerender({ active: true });
    await waitFor(() => expect(result.current.salesReport).toEqual(salesReport));
    rerender({ active: false });
    expect(result.current.salesReport).toEqual(salesReport);
    expect(apiMocks.fetchSalesReport).toHaveBeenCalledOnce();
  });

  it('non esegue query senza i relativi permessi', async () => {
    const { result } = renderDocumentHook({ documentsEnabled: false, reportsEnabled: false });

    await waitFor(() => expect(result.current.documentLoading).toBe(false));
    expect(apiMocks.fetchDocumentPage).not.toHaveBeenCalled();
    expect(apiMocks.fetchSalesReport).not.toHaveBeenCalled();
    expect(apiMocks.fetchInventoryReport).not.toHaveBeenCalled();
  });

  it.each([
    ['401', Object.assign(new Error('Sessione scaduta'), { status: 401 })],
    ['403', Object.assign(new Error('Permesso negato'), { status: 403 })],
    ['500', Object.assign(new Error('Errore server'), { status: 500 })],
    ['offline', new TypeError('Failed to fetch')]
  ])('contiene l errore documenti %s nello stato della slice', async (_, failure) => {
    apiMocks.fetchDocumentPage.mockRejectedValue(failure);
    const { result } = renderDocumentHook({ reportsActive: false });

    await waitFor(() => expect(result.current.documentError).toBe(failure));
    expect(result.current.documentLoading).toBe(false);
  });

  it('mantiene separati gli errori dei due report', async () => {
    const salesFailure = Object.assign(new Error('Vendite non disponibili'), { status: 500 });
    const inventoryFailure = new TypeError('Failed to fetch');
    apiMocks.fetchSalesReport.mockRejectedValue(salesFailure);
    apiMocks.fetchInventoryReport.mockRejectedValue(inventoryFailure);
    const { result } = renderDocumentHook();

    await waitFor(() => expect(result.current.salesError).toBe(salesFailure));
    await waitFor(() => expect(result.current.inventoryError).toBe(inventoryFailure));
    expect(result.current.salesLoading).toBe(false);
    expect(result.current.inventoryLoading).toBe(false);
  });

  it('mantiene la risposta documenti piu recente quando le richieste terminano fuori ordine', async () => {
    const first = deferred<PageResponse<FiscalDocument>>();
    const second = deferred<PageResponse<FiscalDocument>>();
    const creditNote = { ...invoice, id: 2, code: 'NC-2026-0001', type: 'SIMULATED_CREDIT_NOTE' as const, typeLabel: 'Nota di credito simulata' };
    apiMocks.fetchDocumentPage.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const { result } = renderDocumentHook({ reportsActive: false });
    await waitFor(() => expect(apiMocks.fetchDocumentPage).toHaveBeenCalledOnce());

    act(() => result.current.setDocumentQuery({ page: 0, size: 8, type: 'SIMULATED_CREDIT_NOTE' }));
    await waitFor(() => expect(apiMocks.fetchDocumentPage).toHaveBeenCalledTimes(2));
    expect(apiMocks.fetchDocumentPage.mock.calls[0][1].aborted).toBe(true);

    await act(async () => second.resolve(pageOf([creditNote])));
    await waitFor(() => expect(result.current.documentPage.content[0].code).toBe(creditNote.code));
    await act(async () => first.resolve(pageOf([invoice])));
    expect(result.current.documentPage.content[0].code).toBe(creditNote.code);
  });

  it('mantiene il report vendite piu recente quando i filtri terminano fuori ordine', async () => {
    const first = deferred<SalesReport>();
    const second = deferred<SalesReport>();
    const confirmedReport = { ...salesReport, status: 'CONFIRMED' as const, statusLabel: 'Confermato' };
    apiMocks.fetchSalesReport.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    const { result } = renderDocumentHook();
    await waitFor(() => expect(apiMocks.fetchSalesReport).toHaveBeenCalledOnce());

    act(() => result.current.setSalesQuery({ status: 'CONFIRMED' }));
    await waitFor(() => expect(apiMocks.fetchSalesReport).toHaveBeenCalledTimes(2));
    expect(apiMocks.fetchSalesReport.mock.calls[0][1].aborted).toBe(true);

    await act(async () => second.resolve(confirmedReport));
    await waitFor(() => expect(result.current.salesReport?.status).toBe('CONFIRMED'));
    await act(async () => first.resolve(salesReport));
    expect(result.current.salesReport?.status).toBe('CONFIRMED');
  });

  it('applica la fattura confermata e apre la scheda documenti', async () => {
    apiMocks.fetchDocumentPage.mockResolvedValue(pageOf([]));
    const onOpenDocuments = vi.fn();
    const { result } = renderDocumentHook({ reportsActive: false, onOpenDocuments });
    await waitFor(() => expect(result.current.documentLoading).toBe(false));

    await act(async () => result.current.createInvoiceForOrder('ORD-0001'));

    expect(apiMocks.createInvoice).toHaveBeenCalledWith('ORD-0001');
    expect(result.current.documentPage.content).toEqual([invoice]);
    expect(onOpenDocuments).toHaveBeenCalledOnce();
  });

  it('preserva il motivo della nota credito quando il comando fallisce', async () => {
    apiMocks.createCreditNote.mockRejectedValue(new Error('offline'));
    const { result } = renderDocumentHook({ reportsActive: false });
    await waitFor(() => expect(result.current.documentPage.content).toEqual([invoice]));
    act(() => result.current.setCreditReason('Reso autorizzato dal responsabile'));

    await act(async () => result.current.createCreditNoteForOrder('ORD-0001'));

    expect(apiMocks.createCreditNote).toHaveBeenCalledWith('ORD-0001', 'Reso autorizzato dal responsabile');
    expect(result.current.creditReason).toBe('Reso autorizzato dal responsabile');
    expect(result.current.documentPage.content).toEqual([invoice]);
  });

  it('esporta il report selezionato e comunica l esito', async () => {
    const onNotice = vi.fn();
    const { result } = renderDocumentHook({ onNotice });
    await waitFor(() => expect(result.current.salesReport).toEqual(salesReport));

    let exported = false;
    await act(async () => {
      exported = await result.current.exportReport('sales', 'XLSX');
    });

    expect(exported).toBe(true);
    expect(apiMocks.downloadSalesReport).toHaveBeenCalledWith({ status: 'FULFILLED' }, 'XLSX');
    expect(onNotice).toHaveBeenCalledWith('Report vendite esportato in formato XLSX.', 'success', 'Esportazione completata');
  });

  it('blocca due export identici concorrenti', async () => {
    const pending = deferred<void>();
    const onNotice = vi.fn();
    apiMocks.downloadInventoryReport.mockReturnValue(pending.promise);
    const { result } = renderDocumentHook({ onNotice });
    await waitFor(() => expect(result.current.inventoryReport).toEqual(inventoryReport));

    let first!: Promise<boolean>;
    let second!: Promise<boolean>;
    act(() => {
      first = result.current.exportReport('inventory', 'PDF');
      second = result.current.exportReport('inventory', 'PDF');
    });
    await expect(second).resolves.toBe(false);
    expect(apiMocks.downloadInventoryReport).toHaveBeenCalledOnce();
    expect(onNotice).toHaveBeenCalledWith('Il download richiesto e gia in preparazione.', 'info', 'Esportazione in corso');
    await act(async () => pending.resolve());
    await expect(first).resolves.toBe(true);
  });

  it('inoltra l errore export senza perdere i report caricati', async () => {
    const failure = new TypeError('Failed to fetch');
    const onError = vi.fn();
    apiMocks.downloadSalesReport.mockRejectedValue(failure);
    const { result } = renderDocumentHook({ onError });
    await waitFor(() => expect(result.current.salesReport).toEqual(salesReport));

    let exported = true;
    await act(async () => {
      exported = await result.current.exportReport('sales', 'CSV');
    });

    expect(exported).toBe(false);
    expect(onError).toHaveBeenCalledWith(failure);
    expect(result.current.salesReport).toEqual(salesReport);
  });
});

type FlowOverrides = Partial<Parameters<typeof useDocumentReportFlow>[0]>;

function renderDocumentHook(overrides: FlowOverrides = {}) {
  return renderHook(() => useDocumentReportFlow(flowOptions(overrides)));
}

function flowOptions(overrides: FlowOverrides = {}): Parameters<typeof useDocumentReportFlow>[0] {
  return {
    documentsEnabled: true,
    reportsEnabled: true,
    reportsActive: true,
    executeCommand: executorWithoutRefresh,
    onOpenDocuments: vi.fn(),
    onNotice: vi.fn(),
    onError: vi.fn(),
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

function documentFixture(): FiscalDocument {
  return {
    id: 1,
    code: 'FS-2026-0001',
    fiscalYear: 2026,
    sequenceNumber: 1,
    documentPrefix: 'FS',
    type: 'SIMULATED_INVOICE',
    typeLabel: 'Fattura simulata',
    status: 'ISSUED',
    statusLabel: 'Emessa',
    createdAt: '2026-08-18T10:00:00Z',
    relatedOrderCode: 'ORD-0001',
    customer: 'Cliente Test',
    companySnapshotLegalName: 'Negozio Test',
    companySnapshotTaxCode: 'TSTNGZ80A01F839A',
    companySnapshotVatNumber: '01234567890',
    companySnapshotEmail: 'negozio@example.test',
    companySnapshotPhone: '',
    companySnapshotAddress: 'Via Test 1',
    companySnapshotPostalCode: '80100',
    companySnapshotCity: 'Napoli',
    companySnapshotProvince: 'NA',
    companySnapshotCountryCode: 'IT',
    companySnapshotTimeZone: 'Europe/Rome',
    customerSnapshotCode: 'CLI-001',
    customerSnapshotName: 'Cliente Test',
    customerSnapshotTaxCode: '',
    customerSnapshotVatNumber: '',
    customerSnapshotEmail: 'cliente@example.test',
    customerSnapshotPhone: '',
    customerSnapshotAddress: '',
    customerSnapshotCity: 'Napoli',
    paymentMethod: 'CARD',
    taxableAmount: 100,
    vatRate: 0.22,
    vatAmount: 22,
    totalAmount: 122,
    createdBy: 'admin',
    createdByRole: 'ADMIN',
    reason: '',
    disclaimer: 'Documento simulato',
    capabilities: { canCreateCreditNote: true }
  };
}

function salesFixture(): SalesReport {
  return {
    generatedAt: '2026-08-18T10:00:00Z',
    from: '2026-01-01',
    to: '2026-08-18',
    status: 'FULFILLED',
    statusLabel: 'Evaso',
    orderCount: 1,
    orderValue: 122,
    paidAmount: 122,
    refundedAmount: 0,
    netCollectedAmount: 122,
    outstandingAmount: 0,
    averageOrderValue: 122,
    orders: [],
    topProducts: []
  };
}

function inventoryFixture(): InventoryReport {
  return {
    generatedAt: '2026-08-18T10:00:00Z',
    productCount: 1,
    physicalUnits: 2,
    reservedUnits: 0,
    availableUnits: 2,
    potentialRetailStockValue: 200,
    knownInventoryCostValue: 120,
    potentialGrossMarginOnCostedStock: 80,
    costedUnits: 2,
    uncostedUnits: 0,
    costCoveragePercentage: 100,
    lowStockCount: 1,
    outOfStockCount: 0,
    discontinuedCount: 0,
    products: []
  };
}
