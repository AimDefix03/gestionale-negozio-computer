import { act, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { BusinessPartner, PageResponse, SupplierOrder, SupplierOrderSummary } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import { renderHookWithDrafts as renderHook } from '../../test/renderWithDrafts';
import usePurchaseFlow from './usePurchaseFlow';

const apiMocks = vi.hoisted(() => ({
  fetchSupplierOrderPage: vi.fn(),
  fetchSupplierOrder: vi.fn(),
  fetchPartnerPage: vi.fn(),
  createSupplierOrder: vi.fn(),
  sendSupplierOrder: vi.fn(),
  receiveSupplierOrder: vi.fn(),
  cancelSupplierOrder: vi.fn()
}));

vi.mock('../../api', () => apiMocks);

const supplier: BusinessPartner = {
  id: 7,
  code: 'FOR-007',
  type: 'SUPPLIER',
  typeLabel: 'Fornitore',
  displayName: 'Fornitore Test',
  taxCode: '',
  vatNumber: 'IT12345678901',
  email: 'supplier@example.test',
  phone: '',
  address: 'Via Test 1',
  city: 'Napoli',
  notes: '',
  active: true,
  createdAt: '2026-08-27T10:00:00Z',
  updatedAt: '2026-08-27T10:00:00Z'
};

const summary: SupplierOrderSummary = {
  code: 'PO-0001',
  supplierId: supplier.id,
  supplierCode: supplier.code,
  supplierName: supplier.displayName,
  status: 'DRAFT',
  statusLabel: 'Bozza',
  expectedDeliveryDate: '2026-09-03',
  total: 80,
  currency: 'EUR',
  orderedQuantity: 1,
  receivedQuantity: 0,
  createdAt: '2026-08-27T10:00:00Z'
};

const detail: SupplierOrder = {
  ...summary,
  id: 1,
  notes: 'Ordine test',
  createdBy: 'buyer',
  createdByRole: 'Dipendente',
  sentAt: null,
  canceledAt: null,
  canceledBy: null,
  canceledByRole: null,
  cancellationReason: null,
  items: [{ id: 11, productId: 21, productCode: 'GPU-001', productName: 'Scheda video', orderedQuantity: 1, receivedQuantity: 0, remainingQuantity: 1, unitPrice: 80, lineTotal: 80, expectedDeliveryDate: '2026-09-03' }],
  receipts: [],
  capabilities: { canSend: true, canReceive: false, canCancel: true }
};

const executor: CommandExecutor = async <T,>(options: CommandExecutionOptions<T>) => {
  try {
    const value = await options.command();
    options.applyResponse(value);
    await options.afterConfirmed?.(value);
    return { status: 'saved', value };
  } catch (error) {
    return { status: 'failed', error };
  }
};

describe('usePurchaseFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    apiMocks.fetchSupplierOrderPage.mockResolvedValue(pageOf([summary]));
    apiMocks.fetchPartnerPage.mockResolvedValue(pageOf([supplier]));
    apiMocks.fetchSupplierOrder.mockResolvedValue(detail);
    apiMocks.createSupplierOrder.mockResolvedValue(detail);
  });

  it('preserva integralmente la bozza quando la creazione fallisce', async () => {
    apiMocks.createSupplierOrder.mockRejectedValue(new Error('offline'));
    const { result } = renderPurchaseHook();
    await waitFor(() => expect(result.current.suppliers).toEqual([supplier]));
    act(() => result.current.setDraft({
      supplierId: String(supplier.id),
      expectedDeliveryDate: '2026-09-03',
      notes: 'Bozza da non perdere',
      items: [{ productCode: 'GPU-001', quantity: '2', unitPrice: '79.90', expectedDeliveryDate: '' }]
    }));

    await act(async () => result.current.submitDraft());

    expect(result.current.draft).toMatchObject({ supplierId: '7', notes: 'Bozza da non perdere' });
    expect(result.current.draft.items[0]).toMatchObject({ productCode: 'GPU-001', quantity: '2', unitPrice: '79.90' });
    expect(apiMocks.createSupplierOrder).toHaveBeenCalledWith(expect.objectContaining({ supplierId: 7, items: [expect.objectContaining({ quantity: 2, unitPrice: 79.9 })] }));
  });

  it('seleziona la risposta autorevole e pulisce la bozza solo dopo il salvataggio', async () => {
    const { result } = renderPurchaseHook();
    await waitFor(() => expect(result.current.page.content).toEqual([summary]));
    act(() => result.current.setDraft({
      supplierId: String(supplier.id),
      expectedDeliveryDate: '2026-09-03',
      notes: 'Ordine confermato',
      items: [{ productCode: 'GPU-001', quantity: '1', unitPrice: '80.00', expectedDeliveryDate: '' }]
    }));

    await act(async () => result.current.submitDraft());

    expect(result.current.selectedCode).toBe(detail.code);
    expect(result.current.detail).toEqual(detail);
    expect(result.current.draft.supplierId).toBe('');
    expect(result.current.draftDirty).toBe(false);
  });

  it('invia il costo effettivo e aggiorna inventario solo dopo una ricezione confermata', async () => {
    const received: SupplierOrder = {
      ...detail,
      status: 'RECEIVED',
      statusLabel: 'Ricevuto',
      items: [{ ...detail.items[0], receivedQuantity: 1, remainingQuantity: 0 }],
      receipts: [{
        code: 'PR-0001',
        reason: 'Consegna verificata',
        receivedAt: '2026-08-29T10:00:00Z',
        receivedBy: 'buyer',
        receivedByRole: 'Dipendente',
        items: [{ lineId: 11, productCode: 'GPU-001', quantity: 1, expectedUnitCost: 80, actualUnitCost: 77.4321, unitCostVariance: -2.5679, totalCost: 77.4321, inventoryPostingStatus: 'POSTED', stockMovementId: 91 }]
      }]
    };
    apiMocks.receiveSupplierOrder.mockResolvedValue(received);
    const onInventoryChanged = vi.fn().mockResolvedValue(undefined);
    const { result } = renderPurchaseHook(onInventoryChanged);
    await waitFor(() => expect(result.current.page.content).toEqual([summary]));

    await act(async () => result.current.receive('PO-0001', {
      reason: 'Consegna verificata',
      items: [{ lineId: 11, quantity: 1, unitCost: 77.4321 }]
    }));

    expect(apiMocks.receiveSupplierOrder).toHaveBeenCalledWith('PO-0001', {
      reason: 'Consegna verificata',
      items: [{ lineId: 11, quantity: 1, unitCost: 77.4321 }]
    });
    expect(result.current.detail).toEqual(received);
    expect(onInventoryChanged).toHaveBeenCalledTimes(1);
  });
});

function renderPurchaseHook(onInventoryChanged = vi.fn().mockResolvedValue(undefined)) {
  return renderHook(() => usePurchaseFlow({ enabled: true, accountId: 5, executeCommand: executor, onInventoryChanged }));
}

function pageOf<T>(content: T[]): PageResponse<T> {
  return { content, page: 0, size: 8, totalElements: content.length, totalPages: content.length ? 1 : 0, first: true, last: true };
}
