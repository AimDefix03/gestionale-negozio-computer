import { FormEvent } from 'react';
import { InventoryReconciliation, MovementQuery, PageResponse, ProductLookup, StockMovement } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import { MovementFormState } from '../types/ui';
import { dateTime } from '../utils/formatters';
import PhysicalInventoryPanel from '../features/inventory/PhysicalInventoryPanel';
import type { PhysicalInventoryFlowController } from '../features/inventory/usePhysicalInventoryFlow';

type Props = {
  page: PageResponse<StockMovement>;
  query: MovementQuery;
  form: MovementFormState;
  formDirty?: boolean;
  products: ProductLookup[];
  reconciliation: InventoryReconciliation | null;
  busy: boolean;
  pageSize: number;
  onQueryChange: (query: MovementQuery) => void;
  onFormChange: (form: MovementFormState) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  physicalInventoryFlow: PhysicalInventoryFlowController;
};

export default function InventoryPage({ page, query, form, formDirty = false, products, reconciliation, busy, pageSize, onQueryChange, onFormChange, onSubmit, physicalInventoryFlow }: Props) {
  const anomalies = reconciliation?.items.filter((item) => item.status !== 'BALANCED') ?? [];
  const isInitialBalance = form.operation === 'INITIAL_BALANCE';
  const quantityLabel = isInitialBalance ? 'Giacenza iniziale' : 'Quantita';

  return (
    <div className="inventory-workspace">
      <div className="content-grid two">
        <section className="panel">
          <div className="section-heading compact"><span>Operazione ordinaria</span><h2>Movimento di magazzino</h2><p>Carichi, scarichi e saldo iniziale sono registrati nel ledger.</p></div>
          <form className="form-grid single" onSubmit={onSubmit}>
            <label>Prodotto<select value={form.productCode} onChange={(event) => onFormChange({ ...form, productCode: event.target.value })} required><option value="">Seleziona</option>{products.map((product) => <option key={product.code} value={product.code}>{product.code} - {product.name}</option>)}</select></label>
            <label>Operazione<select value={form.operation} onChange={(event) => onFormChange({ ...form, operation: event.target.value as MovementFormState['operation'], quantity: event.target.value === 'INITIAL_BALANCE' ? '0' : '1' })}><option value="INITIAL_BALANCE">Saldo iniziale</option><option value="LOAD">Carico</option><option value="UNLOAD">Scarico</option></select></label>
            <label>{quantityLabel}<input type="number" min={isInitialBalance ? '0' : '1'} step="1" value={form.quantity} onChange={(event) => onFormChange({ ...form, quantity: event.target.value })} required /></label>
            <label>Causale<input value={form.reason} onChange={(event) => onFormChange({ ...form, reason: event.target.value })} placeholder="Motivo verificabile dell'operazione" required /></label>
            <div className="form-actions">{formDirty && <span className="draft-status" role="status">Bozza salvata per questa sessione</span>}<button className="button primary" disabled={busy}>Registra nel ledger</button></div>
          </form>
        </section>
        <DataList
          title="Movimenti magazzino"
          columns={['Tipo', 'Prodotto', 'Quantita', 'Causale', 'Data']}
          rows={page.content.map((movement) => [movement.typeLabel, movement.productCode, `${movement.deltaQuantity > 0 ? '+' : ''}${movement.deltaQuantity} unita`, movement.reason, dateTime.format(new Date(movement.timestamp))])}
          footer={<PaginationControls page={page} onPageChange={(nextPage) => onQueryChange({ ...query, page: nextPage })} />}
        >
          <div className="list-filters">
            <label>Cerca<input value={query.q ?? ''} placeholder="Prodotto, causale o operatore" onChange={(event) => onQueryChange({ ...query, q: event.target.value, page: 0 })} /></label>
            <label>Tipo<select value={query.type ?? 'ALL'} onChange={(event) => onQueryChange({ ...query, type: event.target.value as MovementQuery['type'], page: 0 })}><option value="ALL">Tutti</option><option value="INITIAL_BALANCE">Saldo iniziale</option><option value="PHYSICAL_INVENTORY_INCREASE">Inventario positivo</option><option value="PHYSICAL_INVENTORY_DECREASE">Inventario negativo</option><option value="LOAD">Carico</option><option value="UNLOAD">Scarico</option><option value="RETURN">Reso cliente</option><option value="PURCHASE_RECEIPT">Ricezione fornitore</option></select></label>
            <label>Prodotto<select value={query.productCode ?? ''} onChange={(event) => onQueryChange({ ...query, productCode: event.target.value, page: 0 })}><option value="">Tutti</option>{products.map((product) => <option key={product.code} value={product.code}>{product.code}</option>)}</select></label>
            <button className="button secondary compact-button" type="button" onClick={() => onQueryChange({ page: 0, size: pageSize })}>Reset</button>
          </div>
        </DataList>
      </div>

      <PhysicalInventoryPanel flow={physicalInventoryFlow} products={products} busy={busy} />

      <section className="panel reconciliation-panel">
        <div className="panel-toolbar">
          <div className="section-heading compact"><span>Controllo</span><h2>Riconciliazione giacenze</h2><p>Confronto tra giacenza fisica corrente e movimenti autorevoli.</p></div>
          {reconciliation && <span className={`status-badge ${reconciliation.anomalousProducts === 0 ? 'ok' : 'warning'}`}>{reconciliation.anomalousProducts === 0 ? 'Ledger riconciliato' : `${reconciliation.anomalousProducts} anomalie`}</span>}
        </div>
        {!reconciliation && <p className="empty-state">Riconciliazione non disponibile.</p>}
        {reconciliation && (
          <>
            <div className="reconciliation-summary">
              <div><span>Prodotti</span><strong>{reconciliation.totalProducts}</strong></div>
              <div><span>Riconciliati</span><strong>{reconciliation.balancedProducts}</strong></div>
              <div><span>Da verificare</span><strong>{reconciliation.anomalousProducts}</strong></div>
              <div><span>Movimenti orfani legacy</span><strong>{reconciliation.orphanedLegacyMovements}</strong></div>
            </div>
            {anomalies.length === 0 ? <p className="reconciliation-ok">Tutte le giacenze coincidono con il ledger autorevole.</p> : (
              <div className="reconciliation-list">
                {anomalies.slice(0, 8).map((item) => (
                  <div key={item.productId}>
                    <span><strong>{item.productCode}</strong><small>{item.productName}</small></span>
                    <span>Fisico {item.physicalQuantity}<small>Ledger {item.ledgerQuantity} · Riservato {item.reservedQuantity}</small></span>
                    <span className={`status-badge ${item.status === 'LEDGER_DRIFT' || item.status === 'CHAIN_BROKEN' ? 'critical' : 'warning'}`}>{item.statusLabel}</span>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
