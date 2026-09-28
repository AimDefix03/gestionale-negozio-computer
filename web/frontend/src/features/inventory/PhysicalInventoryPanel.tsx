import { FormEvent, useEffect, useState } from 'react';
import type { PhysicalInventoryItem, ProductLookup } from '../../api';
import PaginationControls from '../../components/PaginationControls';
import { errorMessage } from '../../utils/errors';
import { dateTime } from '../../utils/formatters';
import type { PhysicalInventoryFlowController } from './usePhysicalInventoryFlow';

type Props = {
  flow: PhysicalInventoryFlowController;
  products: ProductLookup[];
  busy: boolean;
};

type CountDraft = { quantity: string; note: string };

export default function PhysicalInventoryPanel({ flow, products, busy }: Props) {
  const [creating, setCreating] = useState(false);
  const [wholeCatalog, setWholeCatalog] = useState(false);
  const [reason, setReason] = useState('');
  const [productCodes, setProductCodes] = useState<string[]>([]);
  const [decisionReason, setDecisionReason] = useState('');
  const [counts, setCounts] = useState<Record<number, CountDraft>>({});

  useEffect(() => {
    if (!flow.detail) {
      setCounts({});
      return;
    }
    setCounts(Object.fromEntries(flow.detail.items.map((item) => [item.id, {
      quantity: String(item.countedQuantity ?? item.theoreticalQuantitySnapshot),
      note: item.countNote ?? ''
    }])));
    setDecisionReason(flow.detail.approvalReason ?? flow.detail.cancellationReason ?? '');
  }, [flow.detail]);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!wholeCatalog && productCodes.length === 0) return;
    await flow.create({ reason, productCodes: wholeCatalog ? [] : productCodes });
    setCreating(false);
    setReason('');
    setProductCodes([]);
    setWholeCatalog(false);
  }

  async function handleCount(item: PhysicalInventoryItem) {
    const draft = counts[item.id];
    await flow.count(item.id, { countedQuantity: Number(draft?.quantity ?? 0), note: draft?.note || undefined });
  }

  const detail = flow.detail;
  const resourceError = flow.error ?? flow.detailError;

  return (
    <section className="panel physical-inventory-panel">
      <div className="panel-toolbar">
        <div className="section-heading compact"><span>Controllo fisico</span><h2>Sessioni di inventario</h2><p>Conta, verifica e approva ogni differenza prima di modificare la giacenza.</p></div>
        <button className="button primary" type="button" onClick={() => setCreating((current) => !current)}>{creating ? 'Chiudi creazione' : 'Nuovo inventario'}</button>
      </div>

      {resourceError ? <div className="resource-error" role="alert"><strong>Dati non aggiornati</strong><span>{errorMessage(resourceError)}</span><button className="button secondary compact-button" type="button" onClick={() => void flow.refreshResources()}>Riprova</button></div> : null}

      {creating && (
        <form className="physical-inventory-create" onSubmit={(event) => void handleCreate(event)}>
          <label className="wide-field">Motivo del conteggio<input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Inventario periodico reparto componenti" maxLength={900} required /></label>
          <label className="checkbox-line"><input type="checkbox" checked={wholeCatalog} onChange={(event) => setWholeCatalog(event.target.checked)} />Includi l’intero catalogo</label>
          <label className="wide-field">Prodotti<select multiple size={Math.min(6, Math.max(3, products.length))} value={productCodes} disabled={wholeCatalog} onChange={(event) => setProductCodes(Array.from(event.currentTarget.selectedOptions, (option) => option.value))}>{products.map((product) => <option key={product.code} value={product.code}>{product.code} · {product.name}</option>)}</select><small>Seleziona uno o più prodotti. L’intero catalogo va scelto esplicitamente.</small></label>
          <div className="form-actions"><span className="selection-summary">{wholeCatalog ? 'Ambito: catalogo completo' : `${productCodes.length} prodotti selezionati`}</span><button className="button primary" disabled={busy || !reason.trim() || (!wholeCatalog && productCodes.length === 0)}>Apri sessione</button></div>
        </form>
      )}

      <div className="physical-inventory-layout">
        <div className="physical-inventory-sessions">
          <div className="list-filters physical-inventory-filters">
            <label>Stato<select value={flow.query.status ?? 'ALL'} onChange={(event) => flow.setQuery({ ...flow.query, status: event.target.value as typeof flow.query.status, page: 0 })}><option value="ALL">Tutti</option><option value="OPEN">Aperti</option><option value="SUBMITTED">Da approvare</option><option value="APPROVED">Approvati</option><option value="CANCELED">Annullati</option></select></label>
          </div>
          {flow.loading && flow.page.content.length === 0 && <p className="empty-state">Caricamento sessioni di inventario.</p>}
          {!flow.loading && flow.page.content.length === 0 && <p className="empty-state">Nessuna sessione presente.</p>}
          <div className="physical-inventory-session-list">
            {flow.page.content.map((session) => (
              <button key={session.id} className={`inventory-session-row${flow.selectedId === session.id ? ' selected' : ''}`} type="button" onClick={() => flow.setSelectedId(session.id)}>
                <span><strong>{session.code}</strong><small>{session.reason}</small></span>
                <span><strong>{session.countedItems}/{session.itemCount}</strong><small>conteggiati</small></span>
                <span className={`status-badge ${statusTone(session.status)}`}>{session.statusLabel}</span>
              </button>
            ))}
          </div>
          <PaginationControls page={flow.page} onPageChange={(page) => flow.setQuery({ ...flow.query, page })} />
        </div>

        <div className="physical-inventory-detail">
          {flow.detailLoading && !detail && <p className="empty-state">Apertura dettaglio inventario.</p>}
          {!flow.detailLoading && !detail && <p className="empty-state">Seleziona una sessione per consultare conteggi, differenze e approvazioni.</p>}
          {detail && (
            <>
              <div className="inventory-detail-header">
                <div><span className="eyebrow">{detail.code}</span><h3>{detail.reason}</h3><p>Aperto da {detail.createdBy} il {dateTime.format(new Date(detail.createdAt))}</p></div>
                <span className={`status-badge ${statusTone(detail.status)}`}>{detail.statusLabel}</span>
              </div>
              <div className="inventory-detail-metrics">
                <span><strong>{detail.itemCount}</strong> righe</span><span><strong>{detail.countedItems}</strong> contate</span><span><strong>{detail.differenceItems}</strong> differenze</span><span><strong>{detail.totalAbsoluteDifference}</strong> unità</span>
              </div>
              <div className="physical-inventory-items">
                {detail.items.map((item) => {
                  const draft = counts[item.id] ?? { quantity: String(item.theoreticalQuantitySnapshot), note: '' };
                  return (
                    <article key={item.id} className="physical-inventory-item">
                      <div className="inventory-item-title"><span><strong>{item.productCode}</strong><small>{item.productName}</small></span><span>Teorico <strong>{item.theoreticalQuantityAtCount ?? item.theoreticalQuantitySnapshot}</strong><small>Riservato {item.reservedQuantityAtCount ?? item.reservedQuantitySnapshot}</small></span></div>
                      <div className="inventory-count-form">
                        <label>Quantità contata<input type="number" min="0" step="1" value={draft.quantity} disabled={!detail.capabilities.canCount} onChange={(event) => setCounts((current) => ({ ...current, [item.id]: { ...draft, quantity: event.target.value } }))} /></label>
                        <label>Nota<input value={draft.note} maxLength={900} disabled={!detail.capabilities.canCount} onChange={(event) => setCounts((current) => ({ ...current, [item.id]: { ...draft, note: event.target.value } }))} placeholder="Posizione o verifica svolta" /></label>
                        {detail.capabilities.canCount && <button className="button secondary compact-button" type="button" disabled={busy || draft.quantity === ''} onClick={() => void handleCount(item)}>Registra</button>}
                      </div>
                      {item.countedQuantity !== null && item.differenceQuantity !== null && <div className={`inventory-difference ${item.differenceQuantity === 0 ? 'balanced' : 'mismatch'}`}><span>Conteggiato {item.countedQuantity}</span><strong>{item.differenceQuantity === 0 ? 'Nessuna differenza' : `Differenza ${signed(item.differenceQuantity)}`}</strong>{item.compensatedMovementDelta !== null && <span>Movimenti compensati {signed(item.compensatedMovementDelta)}</span>}</div>}
                    </article>
                  );
                })}
              </div>
              {(detail.capabilities.canSubmit || detail.capabilities.canApprove || detail.capabilities.canCancel) && (
                <div className="inventory-decision-bar">
                  {(detail.capabilities.canApprove || detail.capabilities.canCancel) && <label>Motivazione decisione<input value={decisionReason} maxLength={900} onChange={(event) => setDecisionReason(event.target.value)} placeholder="Esito della verifica" /></label>}
                  <div className="toolbar-actions">
                    {detail.capabilities.canCancel && <button className="button danger" type="button" disabled={busy || !decisionReason.trim()} onClick={() => void flow.cancel(decisionReason)}>Annulla sessione</button>}
                    {detail.capabilities.canSubmit && <button className="button secondary" type="button" disabled={busy} onClick={() => void flow.submit()}>Invia per approvazione</button>}
                    {detail.capabilities.canApprove && <button className="button primary" type="button" disabled={busy || !decisionReason.trim()} onClick={() => void flow.approve(decisionReason)}>Approva differenze</button>}
                  </div>
                </div>
              )}
              {detail.status === 'SUBMITTED' && !detail.capabilities.canApprove && <p className="workflow-note">L’approvazione richiede un responsabile diverso dall’operatore che ha inviato il conteggio.</p>}
            </>
          )}
        </div>
      </div>
    </section>
  );
}

function statusTone(status: string) {
  if (status === 'APPROVED') return 'ok';
  if (status === 'SUBMITTED') return 'warning';
  if (status === 'CANCELED') return 'canceled';
  return 'info';
}

function signed(value: number) {
  return `${value > 0 ? '+' : ''}${value}`;
}
