import { Order, PageResponse, PaymentMethod, Product, ProductLookup, ProductPayload, ProductQuery, StockMovement } from '../api';
import ProductForm from '../components/ProductForm';
import ProductTable from '../components/ProductTable';
import ProductInsightPanel from '../components/catalog/ProductInsightPanel';
import CartPanel from '../components/orders/CartPanel';
import { CartItem } from '../types/ui';

type Props = {
  productPage: PageResponse<Product>;
  productLookup: ProductLookup[];
  productQuery: ProductQuery;
  selectedCodes: string[];
  focusedProduct: Product | null;
  focusedProductMovements: StockMovement[];
  focusedProductOrders: Order[];
  editingProduct: Product | null;
  cart: CartItem[];
  paymentMethod: PaymentMethod;
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
  onAddToCart: (product: Product) => void;
  onMovement: (product: Product) => void;
  onProductSubmit: (payload: ProductPayload) => Promise<void>;
  onCancelEdit: () => void;
  onCheckout: () => void;
  onPaymentMethodChange: (method: PaymentMethod) => void;
  onClearCart: () => void;
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
        onQueryChange={props.onQueryChange}
        onPageChange={props.onPageChange}
        onSelectionChange={props.onSelectionChange}
        onView={props.onFocusProduct}
        onEdit={props.onEditProduct}
        onDeleteOne={props.onDeleteProduct}
        onDiscontinue={props.onDiscontinueProduct}
        onDeleteSelected={props.onDeleteSelected}
        onAddToCart={props.onAddToCart}
      />
      <aside className="catalog-side">
        <ProductInsightPanel
          product={props.focusedProduct}
          movements={props.focusedProductMovements}
          orders={props.focusedProductOrders}
          canEdit={props.canManageProducts}
          canMove={props.canManageInventory}
          onEdit={props.onEditProduct}
          onMovement={props.onMovement}
          onAddToCart={props.onAddToCart}
        />
        {props.canManageProducts ? (
          <section className="panel form-panel"><ProductForm editingProduct={props.editingProduct} busy={props.busy} onSubmit={props.onProductSubmit} onCancel={props.onCancelEdit} /></section>
        ) : <CartPanel cart={props.cart} paymentMethod={props.paymentMethod} onPaymentMethodChange={props.onPaymentMethodChange} onCheckout={props.onCheckout} onClear={props.onClearCart} />}
      </aside>
    </div>
  );
}
