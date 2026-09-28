import { UserAccount } from '../../api';
import CustomerSalesPage from '../../pages/CustomerSalesPage';
import OrdersPage from '../../pages/OrdersPage';
import SalesPage from '../../pages/SalesPage';
import { errorMessage } from '../../utils/errors';
import type { CatalogFlowController } from '../catalog/useCatalogFlow';
import type { OrderFlowController } from './useOrderFlow';

type Props = {
  view: 'sales' | 'orders';
  flow: OrderFlowController;
  catalog: CatalogFlowController;
  currentUser: UserAccount;
  busy: boolean;
  onInvoice: (orderCode: string) => void;
};

export default function OrderExperience({ view, flow, catalog, currentUser, busy, onInvoice }: Props) {
  if (view === 'sales') {
    const productPage = currentUser.role === 'CUSTOMER' ? catalog.customerProductPage : catalog.productPage;
    const loading = catalog.loading || (currentUser.role !== 'CUSTOMER' && flow.salesCustomersLoading);
    const failure = catalog.error ?? (currentUser.role !== 'CUSTOMER' ? flow.salesCustomersError : null);

    if (loading && productPage.content.length === 0) {
      return <OrderState area="Vendita" title="Caricamento vendita" message="Sto preparando catalogo, disponibilita e destinatari selezionabili." />;
    }
    if (failure && productPage.content.length === 0) {
      return <OrderState area="Vendita" title="Vendita non disponibile" message={errorMessage(failure)} onRetry={() => void refreshSales(flow, catalog)} />;
    }

    return (
      <>
        {failure && (
          <OrderState
            compact
            area="Vendita"
            title="Aggiornamento non riuscito"
            message={`${errorMessage(failure)} I dati gia caricati e la bozza restano disponibili.`}
            onRetry={() => void refreshSales(flow, catalog)}
          />
        )}
        {currentUser.role === 'CUSTOMER' ? (
          <CustomerSalesPage
            productPage={catalog.customerProductPage}
            productQuery={catalog.query}
            username={currentUser.username}
            cart={flow.cart}
            draftDirty={flow.salesDraftDirty}
            paymentMethod={flow.paymentMethod}
            busy={busy || catalog.refreshing}
            onProductQueryChange={catalog.setQuery}
            onProductPageChange={catalog.changePage}
            onAddToCart={flow.addToCart}
            onQuantityChange={flow.changeCartQuantity}
            onPaymentMethodChange={flow.setPaymentMethod}
            onCheckout={() => void flow.checkout()}
            onClearCart={flow.clearDraft}
          />
        ) : (
          <SalesPage
            productPage={catalog.productPage}
            productLookup={catalog.productLookup}
            productQuery={catalog.query}
            customerPage={flow.salesCustomerPage}
            customerQuery={flow.salesCustomerQuery}
            customerMode={flow.salesCustomerMode}
            selectedPartner={flow.selectedSalesPartner}
            walkInCustomerName={flow.walkInCustomerName}
            cart={flow.cart}
            draftDirty={flow.salesDraftDirty}
            paymentMethod={flow.paymentMethod}
            busy={busy || catalog.refreshing || flow.salesCustomersRefreshing}
            onProductQueryChange={catalog.setQuery}
            onProductPageChange={catalog.changePage}
            onCustomerQueryChange={flow.setSalesCustomerQuery}
            onCustomerModeChange={flow.changeCustomerMode}
            onPartnerSelect={flow.setSelectedSalesPartner}
            onWalkInCustomerNameChange={flow.setWalkInCustomerName}
            onAddToCart={flow.addToCart}
            onQuantityChange={flow.changeCartQuantity}
            onPaymentMethodChange={flow.setPaymentMethod}
            onCheckout={() => void flow.checkout()}
            onClearCart={flow.clearDraft}
          />
        )}
      </>
    );
  }

  if (flow.ordersLoading && flow.orderPage.content.length === 0) {
    return <OrderState area="Ordini" title="Caricamento ordini" message="Sto recuperando ordini, pagamenti, resi e riconciliazione." />;
  }
  if (flow.ordersError && flow.orderPage.content.length === 0) {
    return <OrderState area="Ordini" title="Ordini non disponibili" message={errorMessage(flow.ordersError)} onRetry={() => void flow.refreshOrderData()} />;
  }

  return (
    <>
      {flow.ordersError && (
        <OrderState
          compact
          area="Ordini"
          title="Aggiornamento non riuscito"
          message={`${errorMessage(flow.ordersError)} Gli ordini gia caricati restano disponibili.`}
          onRetry={() => void flow.refreshOrderData()}
        />
      )}
      <OrdersPage
        page={flow.orderPage}
        query={flow.orderQuery}
        currentUser={currentUser}
        financialReconciliation={flow.canViewReports ? flow.financialReconciliation : null}
        selectedDetail={flow.selectedOrderDetail}
        detailLoading={flow.orderDetailLoading || flow.orderDetailRefreshing}
        detailError={flow.orderDetailError}
        busy={busy || flow.ordersRefreshing}
        pageSize={flow.orderPage.size}
        onQueryChange={flow.setOrderQuery}
        onSelect={flow.selectOrder}
        onConfirm={(code) => void flow.confirm(code)}
        onFulfill={(code) => void flow.fulfill(code)}
        onCancel={(code, payload) => void flow.cancel(code, payload)}
        onInvoice={onInvoice}
        onReceipt={(code, payload) => void flow.recordReceipt(code, payload)}
        onRequestReturn={flow.requestReturn}
        onApproveReturn={flow.approveReturn}
        onRejectReturn={flow.rejectReturn}
        onReceiveReturn={flow.receiveReturn}
        onRefundReturn={flow.refundReturn}
      />
    </>
  );
}

async function refreshSales(flow: OrderFlowController, catalog: CatalogFlowController) {
  await Promise.all([catalog.refreshProducts(), flow.refreshResources()]);
}

type OrderStateProps = {
  area: string;
  title: string;
  message: string;
  compact?: boolean;
  onRetry?: () => void;
};

function OrderState({ area, title, message, compact = false, onRetry }: OrderStateProps) {
  return (
    <section className={`panel catalog-resource-state${compact ? ' compact' : ''}`} role="status">
      <div className="section-heading compact">
        <span>{area}</span>
        <h2>{title}</h2>
        <p>{message}</p>
      </div>
      {onRetry && <button className="button secondary" type="button" onClick={onRetry}>Riprova</button>}
    </section>
  );
}
