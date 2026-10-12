import { useEffect, useMemo, useState } from 'react';
import {
  createProduct,
  CustomerProduct,
  deleteProduct,
  deleteProducts,
  discontinueProduct,
  fetchCustomerProductPage,
  fetchProductDetail,
  fetchProductLookup,
  fetchProductPage,
  PageResponse,
  Product,
  ProductLookup,
  ProductOperationalDetail,
  ProductPayload,
  ProductQuery,
  StockMovement,
  updateProduct
} from '../../api';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import { commandWasSaved, type CommandExecutor } from '../../hooks/useCommandExecution';
import usePaginatedResource from '../../hooks/usePaginatedResource';
import { removePageItems, upsertPageItem } from '../../utils/pageState';

const DEFAULT_PAGE_SIZE = 8;
const INITIAL_QUERY: ProductQuery = { page: 0, size: DEFAULT_PAGE_SIZE, sort: 'NAME_ASC' };
const LOOKUP_QUERY = { scope: 'internal' };

type CatalogFlowOptions = {
  enabled: boolean;
  customer: boolean;
  executeCommand: CommandExecutor;
  onRefreshDashboard: () => Promise<void>;
};

export default function useCatalogFlow({ enabled, customer, executeCommand, onRefreshDashboard }: CatalogFlowOptions) {
  const [query, setQuery] = useState<ProductQuery>(INITIAL_QUERY);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [focusedProductCode, setFocusedProductCode] = useState('');
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const debouncedSearch = useDebouncedValue(query.q ?? '', 300);
  const requestQuery = useMemo<ProductQuery>(() => ({ ...query, q: debouncedSearch }), [query, debouncedSearch]);
  const emptyInternalPage = useMemo(() => emptyPage<Product>(), []);
  const emptyCustomerPage = useMemo(() => emptyPage<CustomerProduct>(), []);
  const emptyLookup = useMemo<ProductLookup[]>(() => [], []);

  const internalProducts = usePaginatedResource({
    query: requestQuery,
    enabled: enabled && !customer,
    initialData: emptyInternalPage,
    loader: fetchProductPage
  });
  const customerProducts = usePaginatedResource({
    query: requestQuery,
    enabled: enabled && customer,
    initialData: emptyCustomerPage,
    loader: fetchCustomerProductPage
  });
  const productLookup = usePaginatedResource({
    query: LOOKUP_QUERY,
    enabled: enabled && !customer,
    initialData: emptyLookup,
    loader: loadProductLookup
  });
  const productDetail = usePaginatedResource({
    query: { code: focusedProductCode },
    enabled: enabled && !customer && Boolean(focusedProductCode),
    initialData: null as ProductOperationalDetail | null,
    loader: loadProductDetail
  });

  const focusedProduct = useMemo(
    () => productDetail.data?.product
      ?? internalProducts.data.content.find((product) => product.code === focusedProductCode)
      ?? internalProducts.data.content[0]
      ?? null,
    [focusedProductCode, internalProducts.data.content, productDetail.data]
  );

  useEffect(() => {
    if (internalProducts.data.content.length === 0) {
      if (focusedProductCode) setFocusedProductCode('');
      return;
    }
    if (!focusedProductCode || !internalProducts.data.content.some((product) => product.code === focusedProductCode)) {
      setFocusedProductCode(internalProducts.data.content[0].code);
    }
  }, [focusedProductCode, internalProducts.data.content]);

  async function refreshProducts() {
    if (!enabled) return;
    if (customer) {
      await customerProducts.refresh();
      return;
    }
    await Promise.all([internalProducts.refresh(), productLookup.refresh(), productDetail.refresh()]);
  }

  async function refreshAfterProductMutation() {
    await Promise.all([refreshProducts(), onRefreshDashboard()]);
  }

  function applyProduct(product: Product, previousCode?: string) {
    internalProducts.setData((page) => upsertPageItem(page, product, (item) => item.code, previousCode));
    productLookup.setData((products) => {
      const lookup = toProductLookup(product);
      const matchCode = previousCode ?? product.code;
      const existingIndex = products.findIndex((item) => item.code === matchCode);
      if (existingIndex < 0) return [lookup, ...products];
      const updated = [...products];
      updated[existingIndex] = lookup;
      return updated;
    });
    setFocusedProductCode(product.code);
    productDetail.setData((current) => current && (current.product.code === previousCode || current.product.code === product.code)
      ? { ...current, product }
      : current);
  }

  function removeProductsLocally(codes: string[]) {
    const identities = new Set(codes);
    internalProducts.setData((page) => removePageItems(page, identities, (product) => product.code));
    productLookup.setData((products) => products.filter((product) => !identities.has(product.code)));
    setSelectedCodes((selected) => selected.filter((code) => !identities.has(code)));
    setFocusedProductCode((code) => identities.has(code) ? '' : code);
    setEditingProduct((product) => product && identities.has(product.code) ? null : product);
    productDetail.setData((detail) => detail && identities.has(detail.product.code) ? null : detail);
  }

  function applyInventoryMovement(movement: StockMovement) {
    internalProducts.setData((page) => ({
      ...page,
      content: page.content.map((product) => product.code === movement.productCode
        ? {
            ...product,
            quantity: movement.newQuantity,
            availableQuantity: Math.max(0, movement.newQuantity - product.reservedQuantity)
          }
        : product)
    }));
    productLookup.setData((products) => products.map((product) => product.code === movement.productCode
      ? { ...product, availableQuantity: Math.max(0, product.availableQuantity + movement.deltaQuantity) }
      : product));
    productDetail.setData((detail) => detail?.product.code === movement.productCode
      ? {
          ...detail,
          product: {
            ...detail.product,
            quantity: movement.newQuantity,
            availableQuantity: Math.max(0, movement.newQuantity - detail.product.reservedQuantity)
          },
          recentMovements: [movement, ...detail.recentMovements.filter((item) => item.id !== movement.id)].slice(0, 4)
        }
      : detail);
  }

  async function submitProduct(payload: ProductPayload): Promise<boolean> {
    const previousCode = editingProduct?.code;
    const result = await executeCommand({
      key: `product:${previousCode ? 'update' : 'create'}:${previousCode ?? payload.code}`,
      command: () => previousCode ? updateProduct(previousCode, payload) : createProduct(payload),
      applyResponse: (product) => applyProduct(product, previousCode),
      afterConfirmed: () => setEditingProduct(null),
      refresh: refreshAfterProductMutation,
      successMessage: previousCode ? `Prodotto ${payload.code} aggiornato.` : `Prodotto ${payload.code} creato.`
    });
    return commandWasSaved(result);
  }

  async function deleteOne(code: string) {
    if (!window.confirm(`Eliminare il prodotto ${code}?`)) return;
    await executeCommand({
      key: `product:delete:${code}`,
      command: () => deleteProduct(code),
      applyResponse: () => removeProductsLocally([code]),
      refresh: refreshAfterProductMutation,
      successMessage: `Prodotto ${code} eliminato.`
    });
  }

  async function deleteSelected() {
    if (selectedCodes.length === 0 || !window.confirm(`Eliminare ${selectedCodes.length} prodotti?`)) return;
    const codes = [...selectedCodes];
    await executeCommand({
      key: `product:bulk-delete:${codes.slice().sort().join(',')}`,
      command: () => deleteProducts(codes),
      applyResponse: () => removeProductsLocally(codes),
      refresh: refreshAfterProductMutation,
      successMessage: `${codes.length} prodotti eliminati.`
    });
  }

  async function discontinue(code: string) {
    if (!window.confirm(`Disattivare il prodotto ${code}? Non sara disponibile per nuovi ordini.`)) return;
    await executeCommand({
      key: `product:discontinue:${code}`,
      command: () => discontinueProduct(code),
      applyResponse: applyProduct,
      refresh: refreshAfterProductMutation,
      successMessage: `Prodotto ${code} disattivato e mantenuto nello storico.`
    });
  }

  function changeQuery(nextQuery: ProductQuery) {
    setSelectedCodes([]);
    setQuery(nextQuery);
  }

  function changePage(page: number) {
    setQuery((current) => ({ ...current, page }));
  }

  function focusProduct(product: Product) {
    setFocusedProductCode(product.code);
  }

  function editProduct(product: Product) {
    setFocusedProductCode(product.code);
    setEditingProduct(product);
  }

  function reset() {
    setQuery(INITIAL_QUERY);
    setSelectedCodes([]);
    setFocusedProductCode('');
    setEditingProduct(null);
    internalProducts.setData(emptyInternalPage);
    customerProducts.setData(emptyCustomerPage);
    productLookup.setData(emptyLookup);
    productDetail.setData(null);
  }

  return {
    query,
    productPage: internalProducts.data,
    customerProductPage: customerProducts.data,
    productLookup: productLookup.data,
    selectedCodes,
    focusedProduct,
    focusedProductMovements: productDetail.data?.recentMovements ?? [],
    focusedProductOrders: productDetail.data?.recentOrders ?? [],
    detailLoading: productDetail.loading,
    detailRefreshing: productDetail.refreshing,
    detailError: productDetail.error,
    editingProduct,
    loading: customer ? customerProducts.loading : internalProducts.loading || productLookup.loading,
    refreshing: customer ? customerProducts.refreshing : internalProducts.refreshing || productLookup.refreshing,
    error: customer ? customerProducts.error : internalProducts.error ?? productLookup.error,
    setQuery,
    setSelectedCodes,
    changeQuery,
    changePage,
    focusProduct,
    editProduct,
    cancelEdit: () => setEditingProduct(null),
    submitProduct,
    deleteOne,
    deleteSelected,
    discontinue,
    refreshProducts,
    applyInventoryMovement,
    reset
  };
}

export type CatalogFlowController = ReturnType<typeof useCatalogFlow>;

function emptyPage<T>(): PageResponse<T> {
  return { content: [], page: 0, size: DEFAULT_PAGE_SIZE, totalElements: 0, totalPages: 0, first: true, last: true };
}

function loadProductLookup(_: typeof LOOKUP_QUERY, signal: AbortSignal) {
  return fetchProductLookup(signal);
}

function loadProductDetail(query: { code: string }, signal: AbortSignal) {
  return fetchProductDetail(query.code, signal);
}

function toProductLookup(product: Product): ProductLookup {
  return {
    code: product.code,
    name: product.name,
    brand: product.brand,
    productType: product.productType,
    discontinued: product.discontinued,
    availableQuantity: product.availableQuantity
  };
}
