import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { CustomerProduct, PageResponse, Product, ProductPayload } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import useCatalogFlow from './useCatalogFlow';

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

const customerProduct: CustomerProduct = {
  code: product.code,
  name: product.name,
  description: product.description,
  category: product.category,
  brand: product.brand,
  productType: product.productType,
  usageContext: product.usageContext,
  price: product.price,
  discount: product.discount,
  discountedPrice: product.discountedPrice,
  availability: 'AVAILABLE',
  availabilityLabel: 'Disponibile'
};

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

describe('useCatalogFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('confirm', vi.fn(() => true));
    apiMocks.fetchProductPage.mockResolvedValue(pageOf([product]));
    apiMocks.fetchCustomerProductPage.mockResolvedValue(pageOf([customerProduct]));
    apiMocks.fetchProductLookup.mockResolvedValue([toLookup(product)]);
    apiMocks.fetchProductDetail.mockResolvedValue({ product, recentMovements: [], recentOrders: [] });
    apiMocks.createProduct.mockResolvedValue(product);
    apiMocks.updateProduct.mockResolvedValue(product);
    apiMocks.deleteProduct.mockResolvedValue(undefined);
    apiMocks.deleteProducts.mockResolvedValue(undefined);
    apiMocks.discontinueProduct.mockResolvedValue({ ...product, discontinued: true });
  });

  it('carica catalogo operativo e lookup per lo staff', async () => {
    const { result } = renderCatalogHook({ customer: false });

    await waitFor(() => expect(result.current.productPage.content).toEqual([product]));
    expect(result.current.productLookup).toEqual([toLookup(product)]);
    expect(apiMocks.fetchProductPage).toHaveBeenCalledOnce();
    expect(apiMocks.fetchCustomerProductPage).not.toHaveBeenCalled();
    expect(apiMocks.fetchProductPage.mock.calls[0][1]).toBeInstanceOf(AbortSignal);
  });

  it('carica storico e capability dal dettaglio prodotto mirato', async () => {
    const movement = {
      id: 41,
      timestamp: '2026-08-20T10:00:00Z',
      actor: 'operatore',
      role: 'Dipendente',
      productCode: product.code,
      productName: product.name,
      productId: product.id,
      type: 'LOAD' as const,
      typeLabel: 'Carico',
      quantity: 2,
      previousQuantity: 3,
      newQuantity: 5,
      deltaQuantity: 2,
      origin: 'MANUAL_MOVEMENT' as const,
      authoritative: true,
      reason: 'Rifornimento'
    };
    const historicalOrder = {
      code: 'ORD-STORICO',
      customer: 'Cliente storico',
      status: 'FULFILLED' as const,
      statusLabel: 'Evaso',
      timestamp: '2026-08-19T10:00:00Z',
      quantity: 1,
      lineTotal: 810
    };
    apiMocks.fetchProductDetail.mockResolvedValue({ product, recentMovements: [movement], recentOrders: [historicalOrder] });

    const { result } = renderCatalogHook({ customer: false });

    await waitFor(() => expect(result.current.focusedProductMovements).toEqual([movement]));
    expect(result.current.focusedProductOrders).toEqual([historicalOrder]);
    expect(result.current.focusedProduct?.capabilities.canMoveStock).toBe(true);
    expect(apiMocks.fetchProductDetail).toHaveBeenCalledWith(product.code, expect.any(AbortSignal));
  });

  it('carica soltanto la projection commerciale per il cliente', async () => {
    const { result } = renderCatalogHook({ customer: true });

    await waitFor(() => expect(result.current.customerProductPage.content).toEqual([customerProduct]));
    expect(apiMocks.fetchProductPage).not.toHaveBeenCalled();
    expect(apiMocks.fetchProductLookup).not.toHaveBeenCalled();
  });

  it.each([
    ['401', Object.assign(new Error('Sessione scaduta'), { status: 401 })],
    ['403', Object.assign(new Error('Permesso negato'), { status: 403 })],
    ['500', Object.assign(new Error('Errore server'), { status: 500 })],
    ['offline', new TypeError('Failed to fetch')]
  ])('contiene l errore %s nello stato della slice', async (_, failure) => {
    apiMocks.fetchProductPage.mockRejectedValue(failure);
    const { result } = renderCatalogHook({ customer: false });

    await waitFor(() => expect(result.current.error).toBe(failure));
    expect(result.current.loading).toBe(false);
  });

  it('mantiene la risposta piu recente quando due query terminano fuori ordine', async () => {
    const first = deferred<PageResponse<Product>>();
    const second = deferred<PageResponse<Product>>();
    const newerProduct = { ...product, id: 2, code: 'CPU-002', name: 'Processore' };
    apiMocks.fetchProductPage
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise);

    const { result } = renderCatalogHook({ customer: false });
    await waitFor(() => expect(apiMocks.fetchProductPage).toHaveBeenCalledOnce());

    act(() => result.current.setQuery({ page: 0, size: 8, sort: 'PRICE_ASC' }));
    await waitFor(() => expect(apiMocks.fetchProductPage).toHaveBeenCalledTimes(2));
    expect(apiMocks.fetchProductPage.mock.calls[0][1].aborted).toBe(true);

    await act(async () => second.resolve(pageOf([newerProduct])));
    await waitFor(() => expect(result.current.productPage.content[0].code).toBe('CPU-002'));
    await act(async () => first.resolve(pageOf([product])));
    expect(result.current.productPage.content[0].code).toBe('CPU-002');
  });

  it('applica localmente la creazione confermata e chiude la modifica', async () => {
    const created = { ...product, id: 2, code: 'CPU-002', name: 'Processore' };
    apiMocks.createProduct.mockResolvedValue(created);
    const { result } = renderCatalogHook({ customer: false });
    await waitFor(() => expect(result.current.productPage.content).toHaveLength(1));

    let saved = false;
    await act(async () => {
      saved = await result.current.submitProduct(toPayload(created));
    });

    expect(saved).toBe(true);
    expect(result.current.productPage.content.map((item) => item.code)).toContain('CPU-002');
    expect(result.current.productLookup.map((item) => item.code)).toContain('CPU-002');
    expect(result.current.editingProduct).toBeNull();
  });

  it('preserva i dati locali quando il comando prodotto fallisce', async () => {
    apiMocks.createProduct.mockRejectedValue(new Error('offline'));
    const { result } = renderCatalogHook({ customer: false });
    await waitFor(() => expect(result.current.productPage.content).toEqual([product]));

    let saved = true;
    await act(async () => {
      saved = await result.current.submitProduct(toPayload({ ...product, code: 'CPU-002' }));
    });

    expect(saved).toBe(false);
    expect(result.current.productPage.content).toEqual([product]);
  });

  it('rimuove una selezione confermata senza lasciare focus o form obsoleti', async () => {
    const { result } = renderCatalogHook({ customer: false });
    await waitFor(() => expect(result.current.focusedProduct?.code).toBe(product.code));
    act(() => {
      result.current.editProduct(product);
      result.current.setSelectedCodes([product.code]);
    });

    await act(async () => result.current.deleteSelected());

    expect(apiMocks.deleteProducts).toHaveBeenCalledWith([product.code]);
    expect(result.current.productPage.content).toEqual([]);
    expect(result.current.selectedCodes).toEqual([]);
    expect(result.current.focusedProduct).toBeNull();
    expect(result.current.editingProduct).toBeNull();
  });
});

function renderCatalogHook({ customer }: { customer: boolean }) {
  return renderHook(() => useCatalogFlow({
    enabled: true,
    customer,
    executeCommand: executorWithoutRefresh,
    onRefreshDashboard: vi.fn().mockResolvedValue(undefined)
  }));
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

function toLookup(item: Product) {
  return {
    code: item.code,
    name: item.name,
    brand: item.brand,
    productType: item.productType,
    discontinued: item.discontinued,
    availableQuantity: item.availableQuantity
  };
}

function toPayload(item: Product): ProductPayload {
  return {
    code: item.code,
    name: item.name,
    description: item.description,
    category: item.category,
    brand: item.brand,
    productType: item.productType,
    usageContext: item.usageContext,
    price: item.price,
    discount: item.discount
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
