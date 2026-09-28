import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { SupplierOrder } from '../api';
import type { PurchaseFlowController } from '../features/purchases/usePurchaseFlow';
import PurchaseOrdersPage from './PurchaseOrdersPage';

const order: SupplierOrder = {
  id: 1,
  code: 'PO-0001',
  supplierId: 7,
  supplierCode: 'FOR-007',
  supplierName: 'Fornitore Test',
  status: 'SENT',
  statusLabel: 'Inviato',
  expectedDeliveryDate: '2026-09-03',
  notes: '',
  total: 160,
  currency: 'EUR',
  orderedQuantity: 2,
  receivedQuantity: 0,
  createdAt: '2026-08-27T10:00:00Z',
  createdBy: 'buyer',
  createdByRole: 'Dipendente',
  sentAt: '2026-08-27T11:00:00Z',
  canceledAt: null,
  canceledBy: null,
  canceledByRole: null,
  cancellationReason: null,
  items: [{ id: 11, productId: 21, productCode: 'GPU-001', productName: 'Scheda video', orderedQuantity: 2, receivedQuantity: 0, remainingQuantity: 2, unitPrice: 80, lineTotal: 160, expectedDeliveryDate: '2026-09-03' }],
  receipts: [],
  capabilities: { canSend: false, canReceive: true, canCancel: true }
};

describe('PurchaseOrdersPage', () => {
  it('registra quantita, costo effettivo e causale della ricezione fisica', async () => {
    const user = userEvent.setup();
    const receive = vi.fn().mockResolvedValue(undefined);
    render(<PurchaseOrdersPage flow={flow(receive)} products={[]} canManage busy={false} onCreate={vi.fn()} />);

    const quantity = screen.getByLabelText(/GPU-001 · quantita residua 2/);
    await user.clear(quantity);
    await user.type(quantity, '2');
    const cost = screen.getByLabelText(/Costo unitario effettivo/);
    await user.clear(cost);
    await user.type(cost, '77.4321');
    await user.type(screen.getByLabelText('Causale'), 'Consegna verificata');
    await user.click(screen.getByRole('button', { name: 'Registra ricezione' }));

    expect(receive).toHaveBeenCalledWith('PO-0001', {
      reason: 'Consegna verificata',
      items: [{ lineId: 11, quantity: 2, unitCost: 77.4321 }]
    });
    expect(screen.getByText(/Il comando aggiorna giacenza, ledger e costo medio/)).toBeInTheDocument();
  });
});

function flow(receive: ReturnType<typeof vi.fn>): PurchaseFlowController {
  return {
    query: { page: 0, size: 8 },
    page: { content: [], page: 0, size: 8, totalElements: 0, totalPages: 0, first: true, last: true },
    loading: false,
    refreshing: false,
    error: null,
    pageSize: 8,
    selectedCode: order.code,
    detail: order,
    detailLoading: false,
    detailError: null,
    suppliers: [],
    supplierError: null,
    draft: { supplierId: '', expectedDeliveryDate: '2026-09-03', notes: '', items: [{ productCode: '', quantity: '1', unitPrice: '0.00', expectedDeliveryDate: '' }] },
    draftDirty: false,
    setQuery: vi.fn(),
    setSelectedCode: vi.fn(),
    setDraft: vi.fn(),
    clearDraft: vi.fn(),
    submitDraft: vi.fn(),
    send: vi.fn(),
    receive,
    cancel: vi.fn(),
    refreshResources: vi.fn(),
    reset: vi.fn()
  } as PurchaseFlowController;
}
