import { FormEvent, useEffect, useState } from 'react';
import type { ProductLookup, ReceiveSupplierOrderPayload, SupplierOrder, SupplierOrderStatus } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import type { PurchaseFlowController, SupplierOrderDraftLine } from '../features/purchases/usePurchaseFlow';
import { errorMessage } from '../utils/errors';
import { dateTime, money } from '../utils/formatters';

type Props = {
  flow: PurchaseFlowController;
  products: ProductLookup[];
  canManage: boolean;
  busy: boolean;
  onCreate: (event: FormEvent<HTMLFormElement>) => void;
};

export default function PurchaseOrdersPage({ flow, products, canManage, busy, onCreate }: Props) {
  return (
    <div className="purchase-workspace">
      <div className="content-grid purchase-overview">
        <DataList
          title="Ordini fornitore"
          columns={['Codice', 'Fornitore', 'Stato', 'Ricevuto', 'Consegna prevista', 'Totale']}
          rows={flow.page.content.map((order) => [
            order.code,
            <span><strong>{order.supplierName}</strong><small>{order.supplierCode}</small></span>,
            <span className={`status-badge ${statusClass(order.status)}`}>{order.statusLabel}</span>,
            `${order.receivedQuantity} / ${order.orderedQuantity}`,
            localDate(order.expectedDeliveryDate),
            money.format(order.total)
          ])}
          actions={(code) => <button className="link-button" type="button" onClick={() => flow.setSelectedCode(code)}>Apri</button>}
          footer={<PaginationControls page={flow.page} onPageChange={(page) => flow.setQuery({ ...flow.query, page })} />}
          loading={flow.loading}
          refreshing={flow.refreshing}
          error={flow.error ? errorMessage(flow.error) : ''}
          onRetry={() => void flow.refreshResources()}
        >
          <div className="list-filters purchase-filters">
            <label>Cerca<input value={flow.query.q ?? ''} placeholder="Codice o fornitore" onChange={(event) => flow.setQuery({ ...flow.query, q: event.target.value, page: 0 })} /></label>
            <label>Stato<select value={flow.query.status ?? 'ALL'} onChange={(event) => flow.setQuery({ ...flow.query, status: event.target.value as SupplierOrderStatus | 'ALL', page: 0 })}><option value="ALL">Tutti</option><option value="DRAFT">Bozza</option><option value="SENT">Inviato</option><option value="PARTIALLY_RECEIVED">Ricevuto parzialmente</option><option value="RECEIVED">Ricevuto</option><option value="CANCELED">Annullato</option></select></label>
            <label>Fornitore<select value={flow.query.supplierId ?? 'ALL'} onChange={(event) => flow.setQuery({ ...flow.query, supplierId: event.target.value === 'ALL' ? undefined : Number(event.target.value), page: 0 })}><option value="ALL">Tutti</option>{flow.suppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.displayName}</option>)}</select></label>
            <button className="button secondary compact-button" type="button" onClick={() => flow.setQuery({ page: 0, size: flow.pageSize })}>Reset</button>
          </div>
        </DataList>
        <SupplierOrderDraftPanel flow={flow} products={products} canManage={canManage} busy={busy} onCreate={onCreate} />
      </div>
      <SupplierOrderDetailPanel flow={flow} canManage={canManage} busy={busy} />
    </div>
  );
}

function SupplierOrderDraftPanel({ flow, products, canManage, busy, onCreate }: Props) {
  function setLine(index: number, patch: Partial<SupplierOrderDraftLine>) {
    flow.setDraft((current) => ({ ...current, items: current.items.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item) }));
  }

  function addLine() {
    flow.setDraft((current) => ({ ...current, items: [...current.items, { productCode: '', quantity: '1', unitPrice: '0.00', expectedDeliveryDate: '' }] }));
  }

  function removeLine(index: number) {
    flow.setDraft((current) => ({ ...current, items: current.items.filter((_, itemIndex) => itemIndex !== index) }));
  }

  return (
    <section className="panel purchase-create-panel">
      <div className="section-heading compact"><span>Nuovo acquisto</span><h2>Ordine al fornitore</h2><p>Crea una bozza con date, quantita e prezzi concordati.</p></div>
      {canManage ? (
        <form className="purchase-form" onSubmit={onCreate}>
          <div className="form-grid">
            <label>Fornitore<select value={flow.draft.supplierId} onChange={(event) => flow.setDraft({ ...flow.draft, supplierId: event.target.value })} required><option value="">Seleziona fornitore</option>{flow.suppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.displayName} ({supplier.code})</option>)}</select></label>
            <label>Consegna prevista<input type="date" value={flow.draft.expectedDeliveryDate} min={today()} onChange={(event) => flow.setDraft({ ...flow.draft, expectedDeliveryDate: event.target.value })} required /></label>
            <label className="wide">Note<textarea value={flow.draft.notes} maxLength={1200} onChange={(event) => flow.setDraft({ ...flow.draft, notes: event.target.value })} /></label>
          </div>
          <div className="purchase-line-editor">
            <div className="panel-toolbar"><strong>Righe ordine</strong><button className="button secondary compact-button" type="button" onClick={addLine}>Aggiungi riga</button></div>
            {flow.draft.items.map((line, index) => (
              <div className="purchase-draft-line" key={index}>
                <label>Prodotto<select value={line.productCode} onChange={(event) => setLine(index, { productCode: event.target.value })} required><option value="">Seleziona prodotto</option>{products.filter((product) => !product.discontinued).map((product) => <option key={product.code} value={product.code}>{product.name} ({product.code})</option>)}</select></label>
                <label>Quantita<input type="number" min="1" value={line.quantity} onChange={(event) => setLine(index, { quantity: event.target.value })} required /></label>
                <label>Prezzo concordato<input type="number" min="0" step="0.01" value={line.unitPrice} onChange={(event) => setLine(index, { unitPrice: event.target.value })} required /></label>
                <label>Data riga<input type="date" min={today()} value={line.expectedDeliveryDate} onChange={(event) => setLine(index, { expectedDeliveryDate: event.target.value })} /></label>
                <button className="link-button danger-text" type="button" disabled={flow.draft.items.length === 1} onClick={() => removeLine(index)}>Rimuovi</button>
              </div>
            ))}
          </div>
          <div className="form-actions">{flow.draftDirty && <span className="draft-status" role="status">Bozza salvata per questa sessione</span>}<button className="button secondary" type="button" onClick={flow.clearDraft}>Azzera</button><button className="button primary" disabled={busy || flow.suppliers.length === 0}>Crea bozza</button></div>
        </form>
      ) : <div className="empty-state">Permessi insufficienti per creare ordini fornitore.</div>}
    </section>
  );
}

function SupplierOrderDetailPanel({ flow, canManage, busy }: Pick<Props, 'flow' | 'canManage' | 'busy'>) {
  const order = flow.detail;
  const [receiptReason, setReceiptReason] = useState('');
  const [cancelReason, setCancelReason] = useState('');
  const [quantities, setQuantities] = useState<Record<number, string>>({});
  const [unitCosts, setUnitCosts] = useState<Record<number, string>>({});

  useEffect(() => {
    setReceiptReason('');
    setCancelReason('');
    setQuantities({});
    setUnitCosts({});
  }, [order?.code, order?.status]);

  if (!flow.selectedCode) return <section className="panel purchase-detail-empty"><div className="section-heading compact"><span>Dettaglio</span><h2>Seleziona un ordine</h2><p>Apri un ordine per consultare righe, residui, date e ricezioni.</p></div></section>;
  if (flow.detailLoading && !order) return <section className="panel"><div className="resource-status">Caricamento dettaglio ordine...</div></section>;
  if (flow.detailError && !order) return <section className="panel"><div className="resource-error" role="alert">{errorMessage(flow.detailError)}</div></section>;
  if (!order) return null;

  function submitReceipt(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const items = order!.items
      .map((line) => ({
        lineId: line.id,
        quantity: Number(quantities[line.id] ?? 0),
        unitCost: Number(unitCosts[line.id] ?? line.unitPrice)
      }))
      .filter((line) => line.quantity > 0);
    const payload: ReceiveSupplierOrderPayload = { reason: receiptReason, items };
    void flow.receive(order!.code, payload);
  }

  return (
    <section className="panel purchase-detail-panel">
      <div className="panel-toolbar">
        <div className="section-heading compact"><span>Dettaglio ordine</span><h2>{order.code}</h2><p>{order.supplierName} · consegna prevista {localDate(order.expectedDeliveryDate)}</p></div>
        <span className={`status-badge ${statusClass(order.status)}`}>{order.statusLabel}</span>
      </div>
      <div className="purchase-order-meta">
        <div><span>Fornitore</span><strong>{order.supplierCode}</strong></div>
        <div><span>Totale</span><strong>{money.format(order.total)}</strong></div>
        <div><span>Creato da</span><strong>{order.createdBy}</strong></div>
        <div><span>Data</span><strong>{dateTime.format(new Date(order.createdAt))}</strong></div>
      </div>
      <div className="table-shell">
        <table>
          <caption className="sr-only">Righe ordine fornitore {order.code}</caption>
          <thead><tr><th>Prodotto</th><th>Ordinato</th><th>Ricevuto</th><th>Residuo</th><th>Prezzo</th><th>Consegna</th></tr></thead>
          <tbody>{order.items.map((line) => <tr key={line.id}><th scope="row">{line.productName}<small>{line.productCode}</small></th><td>{line.orderedQuantity}</td><td>{line.receivedQuantity}</td><td>{line.remainingQuantity}</td><td>{money.format(line.unitPrice)}</td><td>{localDate(line.expectedDeliveryDate)}</td></tr>)}</tbody>
        </table>
      </div>
      {order.notes && <p className="detail-note"><strong>Note:</strong> {order.notes}</p>}
      {order.cancellationReason && <p className="detail-note warning"><strong>Motivo annullo:</strong> {order.cancellationReason}</p>}
      {canManage && (order.capabilities.canSend || order.capabilities.canReceive || order.capabilities.canCancel) && (
        <div className="purchase-actions-grid">
          {order.capabilities.canSend && <section><h3>Invio ordine</h3><p>Conferma gli snapshot e rende l'ordine disponibile alla ricezione.</p><button className="button primary" disabled={busy} type="button" onClick={() => void flow.send(order.code)}>Segna come inviato</button></section>}
          {order.capabilities.canReceive && <form onSubmit={submitReceipt}><h3>Registra ricezione fisica</h3><p className="field-note">Il comando aggiorna giacenza, ledger e costo medio nella stessa operazione.</p>{order.items.filter((line) => line.remainingQuantity > 0).map((line) => <div className="purchase-receipt-line" key={line.id}><label>{line.productCode} · quantita residua {line.remainingQuantity}<input type="number" min="0" max={line.remainingQuantity} value={quantities[line.id] ?? '0'} onChange={(event) => setQuantities((current) => ({ ...current, [line.id]: event.target.value }))} /></label><label>Costo unitario effettivo<input type="number" min="0" step="0.0001" value={unitCosts[line.id] ?? String(line.unitPrice)} onChange={(event) => setUnitCosts((current) => ({ ...current, [line.id]: event.target.value }))} /><small>Concordato {money.format(line.unitPrice)}</small></label></div>)}<label>Causale<textarea required maxLength={900} value={receiptReason} onChange={(event) => setReceiptReason(event.target.value)} /></label><button className="button primary" disabled={busy || !receiptReason.trim()}>Registra ricezione</button></form>}
          {order.capabilities.canCancel && <form onSubmit={(event) => { event.preventDefault(); void flow.cancel(order.code, cancelReason); }}><h3>Annulla residuo</h3><p>Preserva quanto gia ricevuto e chiude ogni ulteriore ricezione.</p><label>Motivazione<textarea required maxLength={900} value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></label><button className="button danger" disabled={busy || !cancelReason.trim()}>Annulla ordine</button></form>}
        </div>
      )}
      <ReceiptHistory order={order} />
    </section>
  );
}

function ReceiptHistory({ order }: { order: SupplierOrder }) {
  return (
    <div className="purchase-receipts">
      <div className="section-heading compact"><span>Storico</span><h3>Ricezioni merce</h3><p>Quantita, costo effettivo, scostamento e posting nel ledger.</p></div>
      {order.receipts.length ? order.receipts.map((receipt) => <article key={receipt.code}><div><strong>{receipt.code}</strong><span>{dateTime.format(new Date(receipt.receivedAt))} · {receipt.receivedBy}</span></div><p>{receipt.reason}</p><div className="receipt-cost-lines">{receipt.items.map((item) => <span key={item.lineId}><strong>{item.productCode} · {item.quantity} unita</strong><small>Concordato {money.format(item.expectedUnitCost)} · effettivo {money.format(item.actualUnitCost)} · scostamento {signedMoney(item.unitCostVariance)} · totale {money.format(item.totalCost)}</small><em className={`status-badge ${item.inventoryPostingStatus === 'POSTED' ? 'ok' : 'warning'}`}>{item.inventoryPostingStatus === 'POSTED' ? `Movimento #${item.stockMovementId}` : 'Storico non contabilizzato'}</em></span>)}</div></article>) : <div className="empty-state">Nessuna ricezione registrata.</div>}
    </div>
  );
}

function statusClass(status: SupplierOrderStatus) {
  return ({ DRAFT: 'draft', SENT: 'confirmed', PARTIALLY_RECEIVED: 'warning', RECEIVED: 'ok', CANCELED: 'out' } satisfies Record<SupplierOrderStatus, string>)[status];
}

function localDate(value: string) {
  return new Intl.DateTimeFormat('it-IT', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`));
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function signedMoney(value: number) {
  const formatted = money.format(Math.abs(value));
  return value > 0 ? `+${formatted}` : value < 0 ? `-${formatted}` : formatted;
}
