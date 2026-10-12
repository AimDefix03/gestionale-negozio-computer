import { CustomerProduct, PageResponse, PaymentMethod, ProductQuery } from '../api';
import CustomerProductTable from '../components/CustomerProductTable';
import CartPanel from '../components/orders/CartPanel';
import { CartItem } from '../types/ui';

type Props = {
  productPage: PageResponse<CustomerProduct>;
  productQuery: ProductQuery;
  username: string;
  cart: CartItem[];
  draftDirty?: boolean;
  paymentMethod: PaymentMethod;
  busy: boolean;
  onProductQueryChange: (query: ProductQuery) => void;
  onProductPageChange: (page: number) => void;
  onAddToCart: (product: CustomerProduct) => void;
  onQuantityChange: (productCode: string, quantity: number) => void;
  onPaymentMethodChange: (method: PaymentMethod) => void;
  onCheckout: () => void;
  onClearCart: () => void;
};

export default function CustomerSalesPage(props: Props) {
  return (
    <div className="sales-layout">
      <CustomerProductTable
        page={props.productPage}
        query={props.productQuery}
        context="sales"
        busy={props.busy}
        onQueryChange={props.onProductQueryChange}
        onPageChange={props.onProductPageChange}
        onAddToCart={props.onAddToCart}
      />
      <aside className="sales-side">
        <CartPanel
          cart={props.cart}
          draftDirty={props.draftDirty}
          customerLabel={props.username}
          customerReady
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
