import { FormEvent } from 'react';
import type { ProductLookup } from '../../api';
import PurchaseOrdersPage from '../../pages/PurchaseOrdersPage';
import { errorMessage } from '../../utils/errors';
import type { PurchaseFlowController } from './usePurchaseFlow';

type Props = {
  flow: PurchaseFlowController;
  products: ProductLookup[];
  canManage: boolean;
  busy: boolean;
};

export default function PurchaseExperience({ flow, products, canManage, busy }: Props) {
  function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void flow.submitDraft();
  }

  if (flow.loading && flow.page.content.length === 0) {
    return <PurchaseState title="Caricamento approvvigionamenti" message="Sto recuperando ordini, fornitori e residui." />;
  }

  if (flow.error && flow.page.content.length === 0) {
    return <PurchaseState title="Ordini fornitore non disponibili" message={errorMessage(flow.error)} onRetry={() => void flow.refreshResources()} />;
  }

  return (
    <>
      {(flow.error || flow.supplierError) && (
        <PurchaseState compact title="Aggiornamento incompleto" message={`${errorMessage(flow.error ?? flow.supplierError)} I dati gia caricati restano disponibili.`} onRetry={() => void flow.refreshResources()} />
      )}
      <PurchaseOrdersPage
        flow={flow}
        products={products}
        canManage={canManage}
        busy={busy || flow.refreshing}
        onCreate={handleCreate}
      />
    </>
  );
}

function PurchaseState({ title, message, compact = false, onRetry }: { title: string; message: string; compact?: boolean; onRetry?: () => void }) {
  return (
    <section className={`panel purchase-resource-state${compact ? ' compact' : ''}`} role="status">
      <div className="section-heading compact"><span>Acquisti</span><h2>{title}</h2><p>{message}</p></div>
      {onRetry && <button className="button secondary" type="button" onClick={onRetry}>Riprova</button>}
    </section>
  );
}
