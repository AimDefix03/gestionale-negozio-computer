import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import InventoryPage from './InventoryPage';

const emptyPage = { content: [], page: 0, size: 8, totalElements: 0, totalPages: 0, first: true, last: true };
const physicalInventoryFlow = {
  query: { page: 0, size: 6 },
  page: { content: [], page: 0, size: 6, totalElements: 0, totalPages: 0, first: true, last: true },
  loading: false,
  refreshing: false,
  error: null,
  pageSize: 6,
  selectedId: null,
  detail: null,
  detailLoading: false,
  detailError: null,
  setQuery: vi.fn(),
  setSelectedId: vi.fn(),
  create: vi.fn(),
  count: vi.fn(),
  submit: vi.fn(),
  approve: vi.fn(),
  cancel: vi.fn(),
  refreshResources: vi.fn(),
  reset: vi.fn()
};

describe('InventoryPage', () => {
  it('separa i movimenti ordinari dall’inventario approvato e mostra lo stato del ledger', async () => {
    const user = userEvent.setup();
    const onFormChange = vi.fn();
    render(
      <InventoryPage
        page={emptyPage}
        query={{ page: 0, size: 8 }}
        form={{ productCode: 'GPU-001', operation: 'INITIAL_BALANCE', quantity: '0', reason: '' }}
        products={[{ code: 'GPU-001', name: 'Scheda video', brand: 'Brand', productType: 'GPU', discontinued: false, availableQuantity: 0 }]}
        reconciliation={{ generatedAt: '2026-08-05T10:00:00', totalProducts: 1, balancedProducts: 0, anomalousProducts: 1, orphanedLegacyMovements: 0, items: [{ productId: 1, productCode: 'GPU-001', productName: 'Scheda video', physicalQuantity: 0, ledgerQuantity: 0, reservedQuantity: 0, authoritativeMovements: 0, legacyMovements: 0, status: 'MISSING_INITIAL_BALANCE', statusLabel: 'Saldo iniziale assente' }] }}
        busy={false}
        pageSize={8}
        onQueryChange={vi.fn()}
        onFormChange={onFormChange}
        onSubmit={vi.fn()}
        physicalInventoryFlow={physicalInventoryFlow}
      />
    );

    expect(screen.getByText('Riconciliazione giacenze')).toBeInTheDocument();
    expect(screen.getByText('Sessioni di inventario')).toBeInTheDocument();
    expect(screen.getByText('Saldo iniziale assente')).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Rettifica inventario' })).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Operazione'), 'LOAD');
    expect(onFormChange).toHaveBeenCalledWith(expect.objectContaining({ operation: 'LOAD', quantity: '1' }));
  });
});
