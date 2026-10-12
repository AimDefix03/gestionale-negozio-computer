import { FormEvent, useMemo } from 'react';
import { CancellationPayload, DocumentOrderCapabilities, Order, OrderCapabilities, ReceiptPayload, ReturnRefundPayload, ReturnRequestPayload } from '../../api';
import { CommandOutcome } from '../../hooks/useCommandExecution';
import useDraftState from '../../hooks/useDraftState';
import { errorMessage } from '../../utils/errors';
import { dateTime, money } from '../../utils/formatters';

type Props = {
  order: Order | null;
  capabilities: OrderCapabilities | null;
  documents: DocumentOrderCapabilities | null;
  loading: boolean;
  error: unknown;
  busy: boolean;
  onInvoice: (code: string) => void;
  onCancel: (code: string, payload: CancellationPayload) => void;
  onReceipt: (code: string, payload: ReceiptPayload) => void;
  onRequestReturn: (code: string, payload: ReturnRequestPayload) => Promise<CommandOutcome>;
  onApproveReturn: (orderCode: string, returnCode: string, note: string) => Promise<CommandOutcome>;
  onRejectReturn: (orderCode: string, returnCode: string, note: string) => Promise<CommandOutcome>;
  onReceiveReturn: (orderCode: string, returnCode: string) => Promise<CommandOutcome>;
  onRefundReturn: (orderCode: string, returnCode: string, payload: ReturnRefundPayload) => Promise<CommandOutcome>;
};

export default function OrderOperationsPanel(props: Props) {
  const initialDraft = useMemo(() => ({
    receiptAmount: props.order?.payment.outstandingAmount ? String(props.order.payment.outstandingAmount) : '',
    receiptReference: '',
    receiptReason: 'Incasso ordine',
    returnQuantities: {} as Record<string, string>,
    returnReason: '',
    reviewNotes: {} as Record<number, string>,
    refundReturnId: null as number | null,
    refundAmount: '',
    refundReference: '',
    refundReason: 'Rimborso reso ricevuto',
    cancellationReference: '',
    cancellationReason: ''
  }), [props.order?.code, props.order?.payment.outstandingAmount]);
  const { value: draft, setValue: setDraft, dirty: draftDirty } = useDraftState({
    key: `orders:${props.order?.code ?? 'none'}`,
    view: 'orders',
    label: props.order ? `Operazioni ordine ${props.order.code}` : 'Operazioni ordine',
    initialValue: initialDraft,
    enabled: Boolean(props.order)
  });
  const {
    receiptAmount,
    receiptReference,
    receiptReason,
    returnQuantities,
    returnReason,
    reviewNotes,
    refundReturnId,
    refundAmount,
    refundReference,
    refundReason,
    cancellationReference,
    cancellationReason
  } = draft;

  const selectedReturn = useMemo(() => props.order?.returns.find((item) => item.id === refundReturnId) ?? null, [props.order, refundReturnId]);
  const selectedReturnItems = useMemo(() => props.order?.items
    .filter((item) => Number(returnQuantities[item.productCode] ?? 0) > 0)
    .map((item) => ({ productCode: item.productCode, quantity: Number(returnQuantities[item.productCode]) })) ?? [], [props.order, returnQuantities]);

  if (!props.order && props.loading) {
    return <section className="panel order-operations empty-operation"><div className="section-heading compact"><span>Scheda operativa</span><h2>Caricamento ordine</h2><p>Sto verificando stato, permessi e operazioni disponibili.</p></div></section>;
  }

  if (!props.order && props.error) {
    return <section className="panel order-operations empty-operation"><div className="section-heading compact"><span>Scheda operativa</span><h2>Dettaglio non disponibile</h2><p>{errorMessage(props.error)}</p></div></section>;
  }

  if (!props.order || !props.capabilities) {
    return <section className="panel order-operations empty-operation"><div className="section-heading compact"><span>Scheda operativa</span><h2>Seleziona un ordine</h2><p>Apri un ordine dalla tabella per gestire incassi, movimenti e resi.</p></div></section>;
  }

  const canReceivePayment = props.capabilities.canRecordReceipt;
  const canOpenReturn = props.capabilities.canRequestReturn;
  const cancellationRequiresReference = (props.order.payment.netPaidAmount ?? 0) > 0;

  function submitReceipt(event: FormEvent) {
    event.preventDefault();
    props.onReceipt(props.order!.code, { amount: Number(receiptAmount), reference: receiptReference, reason: receiptReason });
  }

  async function submitReturn(event: FormEvent) {
    event.preventDefault();
    if (!selectedReturnItems.length) return;
    const outcome = await props.onRequestReturn(props.order!.code, { reason: returnReason, items: selectedReturnItems });
    if (outcome.saved) {
      setDraft((current) => ({ ...current, returnQuantities: {}, returnReason: '' }));
    }
  }

  async function submitRefund(event: FormEvent) {
    event.preventDefault();
    if (!selectedReturn) return;
    const outcome = await props.onRefundReturn(props.order!.code, selectedReturn.code, { amount: Number(refundAmount), reference: refundReference, reason: refundReason });
    if (outcome.saved) {
      setDraft((current) => ({ ...current, refundReturnId: null, refundAmount: '', refundReference: '', refundReason: 'Rimborso reso ricevuto' }));
    }
  }

  function submitCancellation(event: FormEvent) {
    event.preventDefault();
    props.onCancel(props.order!.code, { reference: cancellationReference, reason: cancellationReason });
  }

  async function reviewReturn(returnId: number, returnCode: string, action: 'approve' | 'reject') {
    const note = reviewNotes[returnId] ?? '';
    const outcome = action === 'approve'
      ? await props.onApproveReturn(props.order!.code, returnCode, note)
      : await props.onRejectReturn(props.order!.code, returnCode, note);
    if (outcome.saved) {
      setDraft((current) => {
        const next = { ...current.reviewNotes };
        delete next[returnId];
        return { ...current, reviewNotes: next };
      });
    }
  }

  async function receiveReturn(returnId: number, returnCode: string) {
    const outcome = await props.onReceiveReturn(props.order!.code, returnCode);
    if (outcome.saved) {
      setDraft((current) => {
        const next = { ...current.reviewNotes };
        delete next[returnId];
        return { ...current, reviewNotes: next };
      });
    }
  }

  return (
    <section className="panel order-operations">
      <div className="section-heading compact operation-heading">
        <span>Scheda operativa</span>
        <h2>{props.order.code}</h2>
        <p>{props.order.customer} · {props.order.statusLabel}</p>
      </div>
      {draftDirty && <p className="draft-status order-draft-status" role="status">Bozza operativa salvata per questa sessione</p>}

      <div className="payment-summary">
        <Metric label="Totale" value={money.format(props.order.payment.requestedAmount)} />
        <Metric label="Incassato" value={knownMoney(props.order.payment.paidAmount)} />
        <Metric label="Rimborsato" value={knownMoney(props.order.payment.refundedAmount)} />
        <Metric label="Residuo" value={knownMoney(props.order.payment.outstandingAmount)} />
      </div>

      {props.documents?.canCreateInvoice && <div className="form-actions insight-actions"><button className="button primary compact-button" disabled={props.busy} onClick={() => props.onInvoice(props.order!.code)}>Genera fattura simulata</button></div>}

      {props.order.payment.reconciliationRequired && (
        <div className="inline-warning" role="status">
          Il saldo storico non dispone di evidenze sufficienti. La situazione contabile deve essere riconciliata da un super admin prima di registrare altri movimenti.
        </div>
      )}

      {props.capabilities.canCancel && (
        <form className="operation-block cancellation-operation" onSubmit={submitCancellation}>
          <div>
            <h3>Annullamento ordine</h3>
            <p className="empty-copy">
              {cancellationRequiresReference
                ? `L'annullamento registra uno storno manuale di ${knownMoney(props.order.payment.netPaidAmount)} e libera le scorte riservate.`
                : "L'annullamento libera le eventuali scorte riservate e conserva la motivazione operativa."}
            </p>
          </div>
          <label>Riferimento storno<input maxLength={120} required={cancellationRequiresReference} value={cancellationReference} onChange={(event) => setDraft((current) => ({ ...current, cancellationReference: event.target.value }))} placeholder={cancellationRequiresReference ? 'Riferimento contabile obbligatorio' : 'Facoltativo'} /></label>
          <label className="wide-field">Motivazione<input maxLength={500} required value={cancellationReason} onChange={(event) => setDraft((current) => ({ ...current, cancellationReason: event.target.value }))} placeholder="Motivazione verificabile" /></label>
          <button className="button danger compact-button" disabled={props.busy}>
            {cancellationRequiresReference ? 'Annulla e registra storno' : 'Annulla ordine'}
          </button>
        </form>
      )}

      {props.order.status === 'CANCELED' && (
        <div className="cancellation-summary">
          <strong>Ordine annullato</strong>
          <span>{props.order.cancellationReason}</span>
          <small>{props.order.canceledAt ? dateTime.format(new Date(props.order.canceledAt)) : ''} · {props.order.canceledBy} · {props.order.canceledByRole}</small>
          {props.order.cancellationReference && <small>Riferimento: {props.order.cancellationReference}</small>}
        </div>
      )}

      <div className="operation-grid">
        <div className="operation-block">
          <h3>Movimenti pagamento</h3>
          <div className="operation-ledger">
            {props.order.payment.transactions.length ? props.order.payment.transactions.map((transaction) => (
              <div className="ledger-row" key={transaction.code}>
                <div><strong>{transaction.typeLabel}</strong><small>{transaction.code} · {dateTime.format(new Date(transaction.recordedAt))}</small></div>
                <span className={transaction.type !== 'RECEIPT' ? 'negative-amount' : ''}>{transaction.type !== 'RECEIPT' ? '-' : '+'}{money.format(transaction.amount)}</span>
              </div>
            )) : <p className="empty-copy">Nessun movimento registrato.</p>}
          </div>
          {canReceivePayment && (
            <form className="compact-operation-form" onSubmit={submitReceipt}>
              <label>Importo<input type="number" min="0.01" step="0.01" max={props.order.payment.outstandingAmount ?? undefined} required value={receiptAmount} onChange={(event) => setDraft((current) => ({ ...current, receiptAmount: event.target.value }))} /></label>
              <label>Riferimento<input maxLength={120} value={receiptReference} onChange={(event) => setDraft((current) => ({ ...current, receiptReference: event.target.value }))} placeholder="POS, CRO o quietanza" /></label>
              <label className="wide-field">Causale<input maxLength={500} required value={receiptReason} onChange={(event) => setDraft((current) => ({ ...current, receiptReason: event.target.value }))} /></label>
              <button className="button primary compact-button" disabled={props.busy}>Registra incasso</button>
            </form>
          )}
        </div>

        <div className="operation-block">
          <h3>Resi</h3>
          {canOpenReturn && (
            <form className="compact-operation-form" onSubmit={submitReturn}>
              <div className="return-builder wide-field">
                <div className="return-builder-heading"><strong>Prodotti da restituire</strong><span>{selectedReturnItems.length} selezionati</span></div>
                {props.order.items.map((item) => (
                  <label className="return-builder-row" key={item.productCode}>
                    <span><strong>{item.productCode} · {item.productName}</strong><small>Ordinati {item.quantity} · Gia resi o impegnati {item.returnedOrReservedQuantity} · Disponibili {item.returnableQuantity}</small></span>
                    <input
                      aria-label={`Quantita da restituire per ${item.productCode}`}
                      type="number"
                      min="0"
                      max={item.returnableQuantity}
                      disabled={item.returnableQuantity === 0}
                      value={returnQuantities[item.productCode] ?? '0'}
                      onChange={(event) => setDraft((current) => ({ ...current, returnQuantities: { ...current.returnQuantities, [item.productCode]: event.target.value } }))}
                    />
                  </label>
                ))}
              </div>
              <label className="wide-field">Motivazione<input maxLength={500} required value={returnReason} onChange={(event) => setDraft((current) => ({ ...current, returnReason: event.target.value }))} placeholder="Motivo verificabile del reso" /></label>
              <button className="button secondary compact-button" disabled={props.busy || selectedReturnItems.length === 0}>Richiedi reso</button>
            </form>
          )}
          <div className="return-list">
            {props.order.returns.length ? props.order.returns.map((orderReturn) => {
              const capability = returnCapability(props.capabilities, orderReturn.code);
              return <article className="return-card" key={orderReturn.id}>
                <div className="return-card-heading"><div><strong>{orderReturn.code}</strong><small>{orderReturn.statusLabel} · {money.format(orderReturn.totalAmount)}</small></div><span>{orderReturn.items.map((item) => `${item.productCode} × ${item.quantity}`).join(', ')}</span></div>
                <p>{orderReturn.reason}</p>
                {orderReturn.reviewNote && <small className="return-review-note">Nota revisione: {orderReturn.reviewNote}</small>}
                {orderReturn.refundTransactions.length > 0 && (
                  <div className="return-refund-ledger">
                    <strong>Rimborsi registrati</strong>
                    {orderReturn.refundTransactions.map((transaction) => (
                      <div className="ledger-row" key={transaction.id}>
                        <div><strong>{transaction.code}</strong><small>{dateTime.format(new Date(transaction.recordedAt))} · {transaction.reference || 'Senza riferimento'}</small><small>{transaction.reason}</small></div>
                        <span className="negative-amount">-{money.format(transaction.amount)}</span>
                      </div>
                    ))}
                  </div>
                )}
                {(capability?.canApprove || capability?.canReject) && <div className="return-actions"><input aria-label={`Nota revisione ${orderReturn.code}`} value={reviewNotes[orderReturn.id] ?? ''} onChange={(event) => setDraft((current) => ({ ...current, reviewNotes: { ...current.reviewNotes, [orderReturn.id]: event.target.value } }))} placeholder={`Nota revisione ${orderReturn.code}`} />{capability.canApprove && <button type="button" className="link-button" disabled={props.busy} onClick={() => void reviewReturn(orderReturn.id, orderReturn.code, 'approve')}>Approva</button>}{capability.canReject && <button type="button" className="link-button danger-text" disabled={props.busy} onClick={() => void reviewReturn(orderReturn.id, orderReturn.code, 'reject')}>Rifiuta</button>}</div>}
                {capability?.canReceive && <button type="button" className="button secondary compact-button" disabled={props.busy} onClick={() => void receiveReturn(orderReturn.id, orderReturn.code)}>Registra ricezione</button>}
                {capability?.canRefund && <button type="button" className="link-button" disabled={props.busy} onClick={() => setDraft((current) => ({ ...current, refundReturnId: orderReturn.id, refundAmount: String(Math.min(orderReturn.refundableAmount, props.order!.payment.refundableAmount ?? 0)) }))}>Prepara rimborso</button>}
              </article>
            }) : <p className="empty-copy">Nessun reso associato.</p>}
          </div>
          {selectedReturn && (
            <form className="compact-operation-form refund-form" onSubmit={submitRefund}>
              <strong>Rimborso {selectedReturn.code}</strong>
              <label>Importo<input type="number" min="0.01" step="0.01" max={Math.min(selectedReturn.refundableAmount, props.order.payment.refundableAmount ?? 0)} required value={refundAmount} onChange={(event) => setDraft((current) => ({ ...current, refundAmount: event.target.value }))} /></label>
              <label>Riferimento<input maxLength={120} value={refundReference} onChange={(event) => setDraft((current) => ({ ...current, refundReference: event.target.value }))} /></label>
              <label className="wide-field">Causale<input maxLength={500} required value={refundReason} onChange={(event) => setDraft((current) => ({ ...current, refundReason: event.target.value }))} /></label>
              <button className="button primary compact-button" disabled={props.busy}>Registra rimborso</button>
            </form>
          )}
        </div>
      </div>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="payment-metric"><span>{label}</span><strong>{value}</strong></div>;
}

function knownMoney(value: number | null) {
  return value === null ? 'Da verificare' : money.format(value);
}

function returnCapability(capabilities: OrderCapabilities | null, returnCode: string) {
  return capabilities?.returns.find((item) => item.returnCode === returnCode);
}
