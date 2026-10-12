import { Product } from '../../api';
import CatalogPage from '../../pages/CatalogPage';
import CustomerCatalogPage from '../../pages/CustomerCatalogPage';
import { errorMessage } from '../../utils/errors';
import type { CatalogFlowController } from './useCatalogFlow';

type Props = {
  flow: CatalogFlowController;
  customer: boolean;
  busy: boolean;
  canManageProducts: boolean;
  canManageInventory: boolean;
  onOpenMovement: (product: Product) => void;
};

export default function CatalogExperience({ flow, customer, busy, canManageProducts, canManageInventory, onOpenMovement }: Props) {
  const activePage = customer ? flow.customerProductPage : flow.productPage;

  if (flow.loading && activePage.content.length === 0) {
    return <CatalogState title="Caricamento catalogo" message="Sto recuperando prodotti, classificazioni e disponibilita." />;
  }

  if (flow.error && activePage.content.length === 0) {
    return <CatalogState title="Catalogo non disponibile" message={errorMessage(flow.error)} onRetry={() => void flow.refreshProducts()} />;
  }

  return (
    <>
      {flow.error && (
        <CatalogState
          compact
          title="Aggiornamento non riuscito"
          message={`${errorMessage(flow.error)} I dati gia caricati restano disponibili.`}
          onRetry={() => void flow.refreshProducts()}
        />
      )}
      {customer ? (
        <CustomerCatalogPage
          page={flow.customerProductPage}
          query={flow.query}
          busy={busy || flow.refreshing}
          onQueryChange={flow.setQuery}
          onPageChange={flow.changePage}
        />
      ) : (
        <CatalogPage
          productPage={flow.productPage}
          productLookup={flow.productLookup}
          productQuery={flow.query}
          selectedCodes={flow.selectedCodes}
          focusedProduct={flow.focusedProduct}
          focusedProductMovements={flow.focusedProductMovements}
          focusedProductOrders={flow.focusedProductOrders}
          detailLoading={flow.detailLoading || flow.detailRefreshing}
          detailError={flow.detailError}
          editingProduct={flow.editingProduct}
          busy={busy || flow.refreshing}
          canManageProducts={canManageProducts}
          canManageInventory={canManageInventory}
          onQueryChange={flow.changeQuery}
          onPageChange={flow.changePage}
          onSelectionChange={flow.setSelectedCodes}
          onFocusProduct={flow.focusProduct}
          onEditProduct={flow.editProduct}
          onDeleteProduct={(code) => void flow.deleteOne(code)}
          onDiscontinueProduct={(code) => void flow.discontinue(code)}
          onDeleteSelected={() => void flow.deleteSelected()}
          onMovement={onOpenMovement}
          onProductSubmit={flow.submitProduct}
          onCancelEdit={flow.cancelEdit}
        />
      )}
    </>
  );
}

type CatalogStateProps = {
  title: string;
  message: string;
  compact?: boolean;
  onRetry?: () => void;
};

function CatalogState({ title, message, compact = false, onRetry }: CatalogStateProps) {
  return (
    <section className={`panel catalog-resource-state${compact ? ' compact' : ''}`} role="status">
      <div className="section-heading compact">
        <span>Catalogo</span>
        <h2>{title}</h2>
        <p>{message}</p>
      </div>
      {onRetry && <button className="button secondary" type="button" onClick={onRetry}>Riprova</button>}
    </section>
  );
}
