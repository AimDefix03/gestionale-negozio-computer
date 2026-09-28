import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import { Order } from '../../api';
import OrderOperationsPanel from './OrderOperationsPanel';
import { renderWithDrafts as render } from '../../test/renderWithDrafts';

const baseOrder: Order = {
  id: 1,
  code: 'ORD-0100',
  customer: 'cliente',
  customerCode: 'CLI-01',
  customerAccountId: 10,
  partnerId: 20,
  customerType: 'SELF_SERVICE',
  customerTypeLabel: 'Cliente autenticato',
  ownershipStatus: 'ACCOUNT',
  ownershipStatusLabel: 'Account collegato',
  timestamp: '2026-07-14T10:00:00',
  paymentMethod: 'Carta',
  total: 100,
  status: 'FULFILLED',
  statusLabel: 'Evaso',
  statusChangedAt: '2026-07-14T10:10:00',
  cancellationReference: null,
  cancellationReason: null,
  canceledAt: null,
  canceledBy: null,
  canceledByRole: null,
  items: [{ productCode: 'GPU-01', productName: 'Scheda video', productDescription: 'Scheda video per test.', quantity: 1, returnedOrReservedQuantity: 0, returnableQuantity: 1, unitPrice: 100, lineTotal: 100 }],
  returns: [],
  payment: {
    id: 1,
    method: 'CARD',
    methodLabel: 'Carta',
    methodDetails: '',
    status: 'PARTIALLY_PAID',
    statusLabel: 'Parzialmente pagato',
    requestedAmount: 100,
    paidAmount: 40,
    refundedAmount: 0,
    netPaidAmount: 40,
    outstandingAmount: 60,
    refundableAmount: 40,
    currency: 'EUR',
    createdAt: '2026-07-14T10:00:00',
    updatedAt: '2026-07-14T10:05:00',
    reconciliationRequired: false,
    reconciledAt: null,
    reconciledBy: null,
    reconciledByRole: null,
    reconciliationReference: null,
    reconciliationReason: null,
    transactions: [{ id: 1, code: 'PAY-0001', type: 'RECEIPT', typeLabel: 'Incasso', amount: 40, reference: 'POS-1', reason: 'Acconto', returnCode: null, returnId: null, cancellationOrderId: null, reconciliationPaymentId: null, recordedAt: '2026-07-14T10:05:00', recordedBy: 'admin', recordedByRole: 'Admin' }]
  },
  capabilities: { canConfirm: false, canFulfill: false, canCancel: false, canRecordReceipt: true, canRequestReturn: true, returns: [] }
};

function handlers() {
  const successfulCommand = () => vi.fn().mockResolvedValue({ status: 'success', saved: true });
  return {
    onReceipt: vi.fn(),
    onRequestReturn: successfulCommand(),
    onApproveReturn: successfulCommand(),
    onRejectReturn: successfulCommand(),
    onReceiveReturn: successfulCommand(),
    onRefundReturn: successfulCommand(),
    onCancel: vi.fn(),
    onInvoice: vi.fn()
  };
}

test('registra incasso e richiesta reso multi-riga dalla scheda ordine', async () => {
  const actions = handlers();
  const order: Order = {
    ...baseOrder,
    items: [
      { ...baseOrder.items[0], quantity: 2, returnedOrReservedQuantity: 1, returnableQuantity: 1 },
      { productCode: 'CPU-01', productName: 'Processore', productDescription: 'Processore per test.', quantity: 3, returnedOrReservedQuantity: 0, returnableQuantity: 3, unitPrice: 80, lineTotal: 240 }
    ]
  };
  render(<OrderOperationsPanel order={order} capabilities={order.capabilities} documents={null} loading={false} error={null} busy={false} {...actions} />);

  fireEvent.change(screen.getByLabelText('Importo'), { target: { value: '60' } });
  fireEvent.click(screen.getByRole('button', { name: 'Registra incasso' }));
  expect(actions.onReceipt).toHaveBeenCalledWith('ORD-0100', expect.objectContaining({ amount: 60, reason: 'Incasso ordine' }));

  fireEvent.change(screen.getByLabelText('Quantita da restituire per GPU-01'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Quantita da restituire per CPU-01'), { target: { value: '2' } });
  fireEvent.change(screen.getByPlaceholderText('Motivo verificabile del reso'), { target: { value: 'Prodotto incompatibile' } });
  fireEvent.click(screen.getByRole('button', { name: 'Richiedi reso' }));
  expect(actions.onRequestReturn).toHaveBeenCalledWith('ORD-0100', { reason: 'Prodotto incompatibile', items: [{ productCode: 'GPU-01', quantity: 1 }, { productCode: 'CPU-01', quantity: 2 }] });
  await waitFor(() => expect(screen.getByPlaceholderText('Motivo verificabile del reso')).toHaveValue(''));
  expect(screen.getByText(/Gia resi o impegnati 1 · Disponibili 1/)).toBeInTheDocument();
});

test('espone le transizioni disponibili per un reso ricevuto', () => {
  const actions = handlers();
  const order: Order = {
    ...baseOrder,
    returns: [{
      id: 101, code: 'RES-0001', status: 'RECEIVED', statusLabel: 'Ricevuto', reason: 'Difetto', totalAmount: 100, refundedAmount: 0, refundableAmount: 100,
      items: baseOrder.items, requestedAt: '2026-07-14T10:20:00', requestedBy: 'cliente', requestedByRole: 'Cliente', reviewNote: '', updatedAt: '2026-07-14T10:30:00',
      refundTransactions: [{ id: 2, code: 'PAY-REF-1', type: 'REFUND', typeLabel: 'Rimborso', amount: 10, reference: 'RIM-OLD', reason: 'Rimborso parziale', returnCode: 'RES-0001', returnId: 101, cancellationOrderId: null, reconciliationPaymentId: null, recordedAt: '2026-07-14T10:35:00', recordedBy: 'admin', recordedByRole: 'Admin' }]
    }],
    capabilities: { ...baseOrder.capabilities, returns: [{ returnCode: 'RES-0001', canApprove: false, canReject: false, canReceive: false, canRefund: true }] }
  };
  render(<OrderOperationsPanel order={order} capabilities={order.capabilities} documents={null} loading={false} error={null} busy={false} {...actions} />);

  expect(screen.getByText('PAY-REF-1')).toBeInTheDocument();
  expect(screen.getByText(/RIM-OLD/)).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Prepara rimborso' }));
  fireEvent.change(screen.getByDisplayValue('Rimborso reso ricevuto'), { target: { value: 'Rimborso autorizzato' } });
  fireEvent.click(screen.getByRole('button', { name: 'Registra rimborso' }));
  expect(actions.onRefundReturn).toHaveBeenCalledWith('ORD-0100', 'RES-0001', expect.objectContaining({ amount: 40, reason: 'Rimborso autorizzato' }));
});

test('rispetta separatamente le capability di approvazione e rifiuto reso', () => {
  const actions = handlers();
  const order: Order = {
    ...baseOrder,
    returns: [{
      id: 102, code: 'RES-0002', status: 'REQUESTED', statusLabel: 'Richiesto', reason: 'Difetto', totalAmount: 100, refundedAmount: 0, refundableAmount: 100,
      items: baseOrder.items, requestedAt: '2026-07-14T10:20:00', requestedBy: 'cliente', requestedByRole: 'Cliente', reviewNote: '', updatedAt: '2026-07-14T10:20:00', refundTransactions: []
    }],
    capabilities: { ...baseOrder.capabilities, returns: [{ returnCode: 'RES-0002', canApprove: false, canReject: true, canReceive: false, canRefund: false }] }
  };
  render(<OrderOperationsPanel order={order} capabilities={order.capabilities} documents={null} loading={false} error={null} busy={false} {...actions} />);

  expect(screen.queryByRole('button', { name: 'Approva' })).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText('Nota revisione RES-0002'), { target: { value: 'Richiesta non conforme' } });
  fireEvent.click(screen.getByRole('button', { name: 'Rifiuta' }));
  expect(actions.onRejectReturn).toHaveBeenCalledWith('ORD-0100', 'RES-0002', 'Richiesta non conforme');
});

test('mantiene separate le note di revisione per identificatore reso', async () => {
  const actions = handlers();
  const createReturn = (id: number, code: string) => ({
    id, code, status: 'REQUESTED' as const, statusLabel: 'Richiesto', reason: `Motivo ${code}`, totalAmount: 100, refundedAmount: 0, refundableAmount: 100,
    items: baseOrder.items, requestedAt: '2026-07-14T10:20:00', requestedBy: 'cliente', requestedByRole: 'Cliente', reviewNote: '', updatedAt: '2026-07-14T10:20:00', refundTransactions: []
  });
  const order: Order = {
    ...baseOrder,
    returns: [createReturn(201, 'RES-0201'), createReturn(202, 'RES-0202')],
    capabilities: {
      ...baseOrder.capabilities,
      returns: [
        { returnCode: 'RES-0201', canApprove: true, canReject: true, canReceive: false, canRefund: false },
        { returnCode: 'RES-0202', canApprove: true, canReject: true, canReceive: false, canRefund: false }
      ]
    }
  };
  render(<OrderOperationsPanel order={order} capabilities={order.capabilities} documents={null} loading={false} error={null} busy={false} {...actions} />);

  fireEvent.change(screen.getByLabelText('Nota revisione RES-0201'), { target: { value: 'Nota primo reso' } });
  fireEvent.change(screen.getByLabelText('Nota revisione RES-0202'), { target: { value: 'Nota secondo reso' } });
  const secondCard = screen.getByText('RES-0202').closest('article');
  fireEvent.click(within(secondCard!).getByRole('button', { name: 'Rifiuta' }));

  expect(actions.onRejectReturn).toHaveBeenCalledWith('ORD-0100', 'RES-0202', 'Nota secondo reso');
  await waitFor(() => expect(screen.getByLabelText('Nota revisione RES-0202')).toHaveValue(''));
  expect(screen.getByLabelText('Nota revisione RES-0201')).toHaveValue('Nota primo reso');
});

test('conserva il builder del reso quando il comando non viene salvato', async () => {
  const actions = handlers();
  actions.onRequestReturn.mockResolvedValue({ status: 'error', saved: false });
  render(<OrderOperationsPanel order={baseOrder} capabilities={baseOrder.capabilities} documents={null} loading={false} error={null} busy={false} {...actions} />);

  fireEvent.change(screen.getByLabelText('Quantita da restituire per GPU-01'), { target: { value: '1' } });
  fireEvent.change(screen.getByPlaceholderText('Motivo verificabile del reso'), { target: { value: 'Motivo da non perdere' } });
  fireEvent.click(screen.getByRole('button', { name: 'Richiedi reso' }));

  await waitFor(() => expect(actions.onRequestReturn).toHaveBeenCalledOnce());
  expect(screen.getByLabelText('Quantita da restituire per GPU-01')).toHaveValue(1);
  expect(screen.getByPlaceholderText('Motivo verificabile del reso')).toHaveValue('Motivo da non perdere');
});

test('distingue un avviso persistito da un invio duplicato non salvato', async () => {
  const savedActions = handlers();
  savedActions.onRequestReturn.mockResolvedValue({ status: 'warning', saved: true });
  const firstRender = render(<OrderOperationsPanel order={baseOrder} capabilities={baseOrder.capabilities} documents={null} loading={false} error={null} busy={false} {...savedActions} />);

  fireEvent.change(screen.getByLabelText('Quantita da restituire per GPU-01'), { target: { value: '1' } });
  fireEvent.change(screen.getByPlaceholderText('Motivo verificabile del reso'), { target: { value: 'Richiesta salvata' } });
  fireEvent.click(screen.getByRole('button', { name: 'Richiedi reso' }));

  await waitFor(() => expect(screen.getByPlaceholderText('Motivo verificabile del reso')).toHaveValue(''));
  firstRender.unmount();

  const duplicateActions = handlers();
  duplicateActions.onRequestReturn.mockResolvedValue({ status: 'warning', saved: false });
  render(<OrderOperationsPanel order={baseOrder} capabilities={baseOrder.capabilities} documents={null} loading={false} error={null} busy={false} {...duplicateActions} />);

  fireEvent.change(screen.getByLabelText('Quantita da restituire per GPU-01'), { target: { value: '1' } });
  fireEvent.change(screen.getByPlaceholderText('Motivo verificabile del reso'), { target: { value: 'Richiesta ancora in corso' } });
  fireEvent.click(screen.getByRole('button', { name: 'Richiedi reso' }));

  await waitFor(() => expect(duplicateActions.onRequestReturn).toHaveBeenCalledOnce());
  expect(screen.getByLabelText('Quantita da restituire per GPU-01')).toHaveValue(1);
  expect(screen.getByPlaceholderText('Motivo verificabile del reso')).toHaveValue('Richiesta ancora in corso');
});

test('non presenta come non pagato un saldo storico da riconciliare', () => {
  const order: Order = {
    ...baseOrder,
    payment: {
      ...baseOrder.payment,
      status: 'UNRECONCILED',
      statusLabel: 'Da riconciliare',
      paidAmount: null,
      refundedAmount: null,
      netPaidAmount: null,
      outstandingAmount: null,
      refundableAmount: null,
      reconciliationRequired: true
    }
  };

  const capabilities = { ...order.capabilities, canRecordReceipt: false };
  render(<OrderOperationsPanel order={order} capabilities={capabilities} documents={null} loading={false} error={null} busy={false} {...handlers()} />);

  expect(screen.getAllByText('Da verificare')).toHaveLength(3);
  expect(screen.getByText(/saldo storico non dispone di evidenze sufficienti/i)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Registra incasso' })).not.toBeInTheDocument();
});

test('richiede riferimento e motivazione prima di annullare un ordine incassato', () => {
  const actions = handlers();
  const order: Order = { ...baseOrder, status: 'CONFIRMED', statusLabel: 'Confermato', capabilities: { ...baseOrder.capabilities, canCancel: true, canRequestReturn: false } };
  render(<OrderOperationsPanel order={order} capabilities={order.capabilities} documents={null} loading={false} error={null} busy={false} {...actions} />);

  fireEvent.change(screen.getByPlaceholderText('Motivazione verificabile'), { target: { value: 'Ordine inserito due volte' } });
  fireEvent.click(screen.getByRole('button', { name: 'Annulla e registra storno' }));
  expect(actions.onCancel).not.toHaveBeenCalled();

  fireEvent.change(screen.getByPlaceholderText('Riferimento contabile obbligatorio'), { target: { value: 'STORNO-2026-01' } });
  fireEvent.click(screen.getByRole('button', { name: 'Annulla e registra storno' }));
  expect(actions.onCancel).toHaveBeenCalledWith('ORD-0100', { reference: 'STORNO-2026-01', reason: 'Ordine inserito due volte' });
});
