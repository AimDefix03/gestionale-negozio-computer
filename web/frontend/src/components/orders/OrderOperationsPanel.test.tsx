import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import { Order } from '../../api';
import OrderOperationsPanel from './OrderOperationsPanel';

const baseOrder: Order = {
  id: 1,
  code: 'ORD-0100',
  customer: 'cliente',
  customerCode: 'CLI-01',
  timestamp: '2026-07-14T10:00:00',
  paymentMethod: 'Carta',
  total: 100,
  status: 'FULFILLED',
  statusLabel: 'Evaso',
  statusChangedAt: '2026-07-14T10:10:00',
  items: [{ productCode: 'GPU-01', productName: 'Scheda video', quantity: 1, unitPrice: 100, lineTotal: 100 }],
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
    transactions: [{ id: 1, code: 'PAY-0001', type: 'RECEIPT', typeLabel: 'Incasso', amount: 40, reference: 'POS-1', reason: 'Acconto', returnCode: '', recordedAt: '2026-07-14T10:05:00', recordedBy: 'admin', recordedByRole: 'Admin' }]
  }
};

function handlers() {
  return {
    onReceipt: vi.fn(),
    onRequestReturn: vi.fn(),
    onApproveReturn: vi.fn(),
    onRejectReturn: vi.fn(),
    onReceiveReturn: vi.fn(),
    onRefundReturn: vi.fn()
  };
}

test('registra incasso e richiesta reso dalla scheda ordine', () => {
  const actions = handlers();
  render(<OrderOperationsPanel order={baseOrder} busy={false} canRecordPayments canRequestReturns canManageReturns canRefundPayments {...actions} />);

  fireEvent.change(screen.getByLabelText('Importo'), { target: { value: '60' } });
  fireEvent.click(screen.getByRole('button', { name: 'Registra incasso' }));
  expect(actions.onReceipt).toHaveBeenCalledWith('ORD-0100', expect.objectContaining({ amount: 60, reason: 'Incasso ordine' }));

  fireEvent.change(screen.getByPlaceholderText('Motivo verificabile del reso'), { target: { value: 'Prodotto incompatibile' } });
  fireEvent.click(screen.getByRole('button', { name: 'Richiedi reso' }));
  expect(actions.onRequestReturn).toHaveBeenCalledWith('ORD-0100', { reason: 'Prodotto incompatibile', items: [{ productCode: 'GPU-01', quantity: 1 }] });
});

test('espone le transizioni disponibili per un reso ricevuto', () => {
  const actions = handlers();
  const order: Order = {
    ...baseOrder,
    returns: [{
      code: 'RES-0001', status: 'RECEIVED', statusLabel: 'Ricevuto', reason: 'Difetto', totalAmount: 100, refundedAmount: 0, refundableAmount: 100,
      items: baseOrder.items, requestedAt: '2026-07-14T10:20:00', requestedBy: 'cliente', requestedByRole: 'Cliente', reviewNote: '', updatedAt: '2026-07-14T10:30:00'
    }]
  };
  render(<OrderOperationsPanel order={order} busy={false} canRecordPayments={false} canRequestReturns canManageReturns canRefundPayments {...actions} />);

  fireEvent.click(screen.getByRole('button', { name: 'Prepara rimborso' }));
  fireEvent.change(screen.getByDisplayValue('Rimborso reso ricevuto'), { target: { value: 'Rimborso autorizzato' } });
  fireEvent.click(screen.getByRole('button', { name: 'Registra rimborso' }));
  expect(actions.onRefundReturn).toHaveBeenCalledWith('ORD-0100', 'RES-0001', expect.objectContaining({ amount: 40, reason: 'Rimborso autorizzato' }));
});
