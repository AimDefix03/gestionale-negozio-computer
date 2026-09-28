import { BusinessPartner, PageResponse, PartnerQuery, PaymentMethod, Product, ProductLookup, ProductQuery } from '../api';
import ProductTable from '../components/ProductTable';
import CartPanel from '../components/orders/CartPanel';
import SalesCustomerPanel from '../components/orders/SalesCustomerPanel';
import { CartItem, SalesCustomerMode } from '../types/ui';

type Props = {
  productPage: PageResponse<Product>;
  productLookup: ProductLookup[];
  productQuery: ProductQuery;
  customerPage: PageResponse<BusinessPartner>;
  customerQuery: PartnerQuery;
  selfServiceUsername?: string;
  customerMode: SalesCustomerMode;
  selectedPartner?: BusinessPartner;
  walkInCustomerName: string;
  cart: CartItem[];
  draftDirty?: boolean;
  paymentMethod: PaymentMethod;
  busy: boolean;
  onProductQueryChange: (query: ProductQuery) => void;
  onProductPageChange: (page: number) => void;
  onCustomerQueryChange: (query: PartnerQuery) => void;
  onCustomerModeChange: (mode: SalesCustomerMode) => void;
  onPartnerSelect: (partner: BusinessPartner) => void;
  onWalkInCustomerNameChange: (name: string) => void;
  onAddToCart: (product: Product) => void;
  onQuantityChange: (productCode: string, quantity: number) => void;
  onPaymentMethodChange: (method: PaymentMethod) => void;
  onCheckout: () => void;
  onClearCart: () => void;
};

export default function SalesPage(props: Props) {
  const customerLabel = props.selfServiceUsername
    ?? (props.customerMode === 'REGISTERED' ? props.selectedPartner?.displayName : props.walkInCustomerName.trim())
    ?? '';
  const customerReady = Boolean(customerLabel);

  return (
    <div className="sales-layout">
      <ProductTable
        page={props.productPage}
        filterSource={props.productLookup}
        query={props.productQuery}
        selectedCodes={[]}
        canManage={false}
        canAddToCart
        context="sales"
        busy={props.busy}
        onQueryChange={props.onProductQueryChange}
        onPageChange={props.onProductPageChange}
        onSelectionChange={() => undefined}
        onEdit={() => undefined}
        onDeleteOne={() => undefined}
        onDiscontinue={() => undefined}
        onDeleteSelected={() => undefined}
        onAddToCart={props.onAddToCart}
      />
      <aside className="sales-side">
        <SalesCustomerPanel
          selfServiceUsername={props.selfServiceUsername}
          mode={props.customerMode}
          page={props.customerPage}
          query={props.customerQuery}
          selectedPartnerId={props.selectedPartner?.id}
          walkInCustomerName={props.walkInCustomerName}
          busy={props.busy}
          onModeChange={props.onCustomerModeChange}
          onQueryChange={props.onCustomerQueryChange}
          onPartnerSelect={props.onPartnerSelect}
          onWalkInCustomerNameChange={props.onWalkInCustomerNameChange}
        />
        <CartPanel
          cart={props.cart}
          draftDirty={props.draftDirty}
          customerLabel={customerLabel || 'Cliente da selezionare'}
          customerReady={customerReady}
          paymentMethod={props.paymentMethod}
          busy={props.busy}
          onPaymentMethodChange={props.onPaymentMethodChange}
          onQuantityChange={props.onQuantityChange}
          onCheckout={props.onCheckout}
          onClear={props.onClearCart}
        />
      </aside>
    </div>
  );
}
