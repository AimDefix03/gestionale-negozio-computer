import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { InventoryReport, SalesReport } from '../api';
import ReportsPage from './ReportsPage';

const sales: SalesReport = {
  generatedAt: '2026-07-14T10:30:00',
  from: '2026-01-01',
  to: '2026-07-14',
  status: 'FULFILLED',
  statusLabel: 'Evaso',
  orderCount: 1,
  orderValue: 120,
  paidAmount: 100,
  refundedAmount: 10,
  netCollectedAmount: 90,
  outstandingAmount: 20,
  averageOrderValue: 120,
  orders: [{ code: 'ORD-1', timestamp: '2026-07-13T10:00:00', customer: 'Cliente Uno', status: 'FULFILLED', statusLabel: 'Evaso', total: 120, paidAmount: 100, refundedAmount: 10, netCollectedAmount: 90, outstandingAmount: 20 }],
  topProducts: [{ productCode: 'GPU-1', productName: 'Scheda grafica', quantity: 1, orderValue: 120 }]
};

const inventory: InventoryReport = {
  generatedAt: '2026-07-14T10:30:00',
  productCount: 1,
  physicalUnits: 4,
  reservedUnits: 1,
  availableUnits: 3,
  inventoryValue: 432,
  lowStockCount: 1,
  outOfStockCount: 0,
  discontinuedCount: 0,
  products: [{ code: 'GPU-1', name: 'Scheda grafica', category: 'HARDWARE', categoryLabel: 'Hardware', brand: 'Brand', productType: 'Scheda grafica', quantity: 4, reservedQuantity: 1, availableQuantity: 3, price: 120, discount: 10, discountedPrice: 108, stockValue: 432, discontinued: false, stockStatus: 'LOW', stockStatusLabel: 'Scorta bassa' }]
};

function setup() {
  const callbacks = {
    onSalesQueryChange: vi.fn(),
    onInventoryQueryChange: vi.fn(),
    onExportSales: vi.fn(),
    onExportInventory: vi.fn(),
    onRefreshSales: vi.fn(),
    onRefreshInventory: vi.fn()
  };
  render(<ReportsPage sales={sales} inventory={inventory} salesQuery={{ status: 'FULFILLED' }} inventoryQuery={{ stock: 'ALL', discontinued: false }} busy={false} {...callbacks} />);
  return callbacks;
}

describe('ReportsPage', () => {
  it('mostra metriche e dettagli vendita e avvia un export Excel', async () => {
    const user = userEvent.setup();
    const callbacks = setup();

    expect(screen.getByRole('heading', { name: 'Vendite e incassi' })).toBeInTheDocument();
    expect(screen.getByText('ORD-1')).toBeInTheDocument();
    expect(screen.getByText('Scheda grafica')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Excel' }));
    expect(callbacks.onExportSales).toHaveBeenCalledWith('XLSX');
    await user.selectOptions(screen.getByLabelText('Stato'), 'CONFIRMED');
    expect(callbacks.onSalesQueryChange).toHaveBeenCalledWith({ status: 'CONFIRMED' });
  });

  it('passa allo snapshot magazzino e mantiene separato il download PDF', async () => {
    const user = userEvent.setup();
    const callbacks = setup();

    await user.click(screen.getByRole('button', { name: 'Magazzino' }));
    expect(screen.getByRole('heading', { name: 'Snapshot di magazzino' })).toBeInTheDocument();
    expect(screen.getByText('GPU-1 · Brand')).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Scorta bassa' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'PDF' }));
    expect(callbacks.onExportInventory).toHaveBeenCalledWith('PDF');
  });
});
