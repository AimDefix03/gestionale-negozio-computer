import { PageResponse, Product, ProductLookup, ProductOrderHistory, ProductPayload, ProductQuery, StockMovement } from '../api';
import ProductForm from '../components/ProductForm';
import ProductTable from '../components/ProductTable';
import ProductInsightPanel from '../components/catalog/ProductInsightPanel';

type Props = {
  productPage: PageResponse<Product>;
  productLookup: ProductLookup[];
  productQuery: ProductQuery;
  selectedCodes: string[];
  focusedProduct: Product | null;
  focusedProductMovements: StockMovement[];
  focusedProductOrders: ProductOrderHistory[];
  detailLoading: boolean;
  detailError: unknown;
  editingProduct: Product | null;
  busy: boolean;
  canManageProducts: boolean;
  canManageInventory: boolean;
  onQueryChange: (query: ProductQuery) => void;
  onPageChange: (page: number) => void;
  onSelectionChange: (codes: string[]) => void;
  onFocusProduct: (product: Product) => void;
  onEditProduct: (product: Product) => void;
  onDeleteProduct: (code: string) => void;
  onDiscontinueProduct: (code: string) => void;
  onDeleteSelected: () => void;
  onMovement: (product: Product) => void;
  onProductSubmit: (payload: ProductPayload) => Promise<boolean>;
  onCancelEdit: () => void;
};

export default function CatalogPage(props: Props) {
  return (
    <div className="content-grid catalog-layout">
      <ProductTable
        page={props.productPage}
        filterSource={props.productLookup}
        query={props.productQuery}
        selectedCodes={props.selectedCodes}
        activeProductCode={props.focusedProduct?.code}
        canManage={props.canManageProducts}
        canAddToCart={false}
        busy={props.busy}
        onQueryChange={props.onQueryChange}
        onPageChange={props.onPageChange}
        onSelectionChange={props.onSelectionChange}
        onView={props.onFocusProduct}
        onEdit={props.onEditProduct}
        onDeleteOne={props.onDeleteProduct}
        onDiscontinue={props.onDiscontinueProduct}
        onDeleteSelected={props.onDeleteSelected}
      />
      <aside className="catalog-side">
        <ProductInsightPanel
          product={props.focusedProduct}
          movements={props.focusedProductMovements}
          orders={props.focusedProductOrders}
          loading={props.detailLoading}
          error={props.detailError}
          canEdit={props.canManageProducts && Boolean(props.focusedProduct?.capabilities.canEdit)}
          canMove={props.canManageInventory && Boolean(props.focusedProduct?.capabilities.canMoveStock)}
          onEdit={props.onEditProduct}
          onMovement={props.onMovement}
        />
        {props.canManageProducts && (
          <section className="panel form-panel"><ProductForm editingProduct={props.editingProduct} busy={props.busy} onSubmit={props.onProductSubmit} onCancel={props.onCancelEdit} /></section>
        )}
      </aside>
    </div>
  );
}
