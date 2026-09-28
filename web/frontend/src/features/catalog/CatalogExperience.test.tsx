import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it, vi } from 'vitest';
import type { PageResponse, Product } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import CatalogExperience from './CatalogExperience';
import useCatalogFlow from './useCatalogFlow';
import { renderWithDrafts as render } from '../../test/renderWithDrafts';

const apiMocks = vi.hoisted(() => ({
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  deleteProduct: vi.fn(),
  deleteProducts: vi.fn(),
  discontinueProduct: vi.fn(),
  fetchProductPage: vi.fn(),
  fetchCustomerProductPage: vi.fn(),
  fetchProductLookup: vi.fn(),
  fetchProductDetail: vi.fn()
}));

vi.mock('../../api', () => apiMocks);

const product: Product = {
  id: 1,
  code: 'GPU-001',
  name: 'Scheda video',
  description: 'Prodotto di test',
  category: 'HARDWARE',
  brand: 'Example Brand',
  productType: 'GPU',
  usageContext: 'Gaming',
  quantity: 5,
  reservedQuantity: 1,
  availableQuantity: 4,
  lastPurchaseCost: 500, averagePurchaseCost: 500, costedQuantity: 5, uncostedQuantity: 0, costCoveragePercentage: 100, knownInventoryCost: 2500, potentialGrossMarginOnCostedStock: 1550,
  price: 900,
  discount: 10,
  discountedPrice: 810,
  discontinued: false,
  capabilities: { canEdit: true, canChangeCode: true, canDelete: true, canDiscontinue: true, canMoveStock: true }
};

const executeCommand: CommandExecutor = async <T,>(options: CommandExecutionOptions<T>) => {
  const value = await options.command();
  options.applyResponse(value);
  options.afterConfirmed?.(value);
  return { status: 'saved', value };
};

it('compone tabella, dettaglio e form senza stato catalogo in App', async () => {
  const user = userEvent.setup();
  apiMocks.fetchProductPage.mockResolvedValue(pageOf([product]));
  apiMocks.fetchProductLookup.mockResolvedValue([{
    code: product.code,
    name: product.name,
    brand: product.brand,
    productType: product.productType,
    discontinued: product.discontinued,
    availableQuantity: product.availableQuantity
  }]);
  apiMocks.fetchProductDetail.mockResolvedValue({ product, recentMovements: [], recentOrders: [] });

  render(<CatalogHarness />);

  await waitFor(() => expect(screen.getByRole('heading', { name: 'Catalogo prodotti' })).toBeInTheDocument());
  expect(screen.getAllByText('Scheda video').length).toBeGreaterThan(0);
  await user.click(screen.getAllByRole('button', { name: 'Modifica' })[0]);
  expect(screen.getByRole('heading', { name: 'Modifica prodotto' })).toBeInTheDocument();
  expect(screen.getByLabelText('Codice')).toHaveValue('GPU-001');
});

function CatalogHarness() {
  const flow = useCatalogFlow({
    enabled: true,
    customer: false,
    executeCommand,
    onRefreshDashboard: vi.fn().mockResolvedValue(undefined)
  });
  return (
    <CatalogExperience
      flow={flow}
      customer={false}
      busy={false}
      canManageProducts
      canManageInventory
      onOpenMovement={vi.fn()}
    />
  );
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
