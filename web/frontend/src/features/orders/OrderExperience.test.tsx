import { screen, waitFor } from '@testing-library/react';
import { expect, it, vi } from 'vitest';
import type { BusinessPartner, PageResponse, Product, UserAccount } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import useCatalogFlow from '../catalog/useCatalogFlow';
import OrderExperience from './OrderExperience';
import useOrderFlow from './useOrderFlow';
import { renderWithDrafts as render } from '../../test/renderWithDrafts';

const apiMocks = vi.hoisted(() => ({
  approveOrderReturn: vi.fn(),
  cancelOrder: vi.fn(),
  confirmOrder: vi.fn(),
  createOrder: vi.fn(),
  fetchFinancialReconciliation: vi.fn(),
  fetchOrderPage: vi.fn(),
  fetchOrderDetail: vi.fn(),
  fetchPartnerPage: vi.fn(),
  fulfillOrder: vi.fn(),
  receiveOrderReturn: vi.fn(),
  recordOrderReceipt: vi.fn(),
  refundOrderReturn: vi.fn(),
  rejectOrderReturn: vi.fn(),
  requestOrderReturn: vi.fn(),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  deleteProduct: vi.fn(),
  deleteProducts: vi.fn(),
  discontinueProduct: vi.fn(),
  fetchProductPage: vi.fn(),
  fetchCustomerProductPage: vi.fn(),
  fetchProductLookup: vi.fn()
  , fetchProductDetail: vi.fn()
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

const partner: BusinessPartner = {
  id: 42,
  code: 'CLI-001',
  type: 'CUSTOMER',
  typeLabel: 'Cliente',
  displayName: 'Cliente censito',
  taxCode: '',
  vatNumber: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  notes: '',
  active: true,
  createdAt: '2026-08-18T09:00:00Z',
  updatedAt: '2026-08-18T09:00:00Z'
};

const currentUser: UserAccount = {
  id: 10,
  username: 'operatore',
  role: 'EMPLOYEE',
  roleLabel: 'Dipendente',
  permissions: ['VIEW_ORDERS', 'CREATE_ORDERS', 'CONFIRM_ORDERS', 'FULFILL_ORDERS', 'CANCEL_ORDERS', 'RECORD_PAYMENTS', 'REFUND_PAYMENTS', 'REQUEST_RETURNS', 'MANAGE_RETURNS', 'MANAGE_DOCUMENTS', 'VIEW_REPORTS'],
  enabled: true,
  disabledAt: null,
  disabledBy: null,
  disabledReason: null
};

const executeCommand: CommandExecutor = async <T,>(options: CommandExecutionOptions<T>) => {
  const value = await options.command();
  options.applyResponse(value);
  options.afterConfirmed?.(value);
  return { status: 'saved', value };
};

it('compone vendita e ordini senza stato di dominio in App', async () => {
  apiMocks.fetchOrderPage.mockResolvedValue(pageOf([]));
  apiMocks.fetchPartnerPage.mockResolvedValue(pageOf([partner], 6));
  apiMocks.fetchFinancialReconciliation.mockResolvedValue({ generatedAt: '2026-08-18T09:00:00Z', balanced: true, checkedPayments: 0, checkedReturns: 0, mismatchCount: 0, counts: {}, mismatches: [] });
  apiMocks.fetchProductPage.mockResolvedValue(pageOf([product]));
  apiMocks.fetchProductLookup.mockResolvedValue([{ code: product.code, name: product.name, brand: product.brand, productType: product.productType, discontinued: false, availableQuantity: 4 }]);
  apiMocks.fetchProductDetail.mockResolvedValue({ product, recentMovements: [], recentOrders: [] });

  const { rerender } = render(<OrderHarness view="orders" />);

  await waitFor(() => expect(screen.getByRole('heading', { name: 'Ordini' })).toBeInTheDocument());
  rerender(<OrderHarness view="sales" />);
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Bozza ordine' })).toBeInTheDocument());
  expect(screen.getByText('Scheda video')).toBeInTheDocument();
  expect(screen.getAllByText('Cliente censito').length).toBeGreaterThan(0);
});

function OrderHarness({ view }: { view: 'sales' | 'orders' }) {
  const catalog = useCatalogFlow({
    enabled: true,
    customer: false,
    executeCommand,
    onRefreshDashboard: vi.fn().mockResolvedValue(undefined)
  });
  const flow = useOrderFlow({
    enabled: true,
    currentUser,
    executeCommand,
    onRefreshDashboard: vi.fn().mockResolvedValue(undefined),
    onRefreshInventory: vi.fn().mockResolvedValue(undefined),
    onOpenOrders: vi.fn(),
    onNotice: vi.fn()
  });
  return <OrderExperience view={view} flow={flow} catalog={catalog} currentUser={currentUser} busy={false} onInvoice={vi.fn()} />;
}

function pageOf<T>(content: T[], size = 8): PageResponse<T> {
  return {
    content,
    page: 0,
    size,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true
  };
}
