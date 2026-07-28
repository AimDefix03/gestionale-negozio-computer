import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Order, ReceiptPayload, ReturnRefundPayload, ReturnRequestPayload } from '../../api';
import { dateTime, money } from '../../utils/formatters';

type Props = {
  order: Order | null;
  busy: boolean;
  canRecordPayments: boolean;
  canRequestReturns: boolean;
  canManageReturns: boolean;
  canRefundPayments: boolean;
  onReceipt: (code: string, payload: ReceiptPayload) => void;
  onRequestReturn: (code: string, payload: ReturnRequestPayload) => void;
  onApproveReturn: (orderCode: string, returnCode: string, note: string) => void;
  onRejectReturn: (orderCode: string, returnCode: string, note: string) => void;
  onReceiveReturn: (orderCode: string, returnCode: string) => void;
  onRefundReturn: (orderCode: string, returnCode: string, payload: ReturnRefundPayload) => void;
};

export default function OrderOperationsPanel(props: Props) {
  const [receiptAmount, setReceiptAmount] = useState('');
  const [receiptReference, setReceiptReference] = useState('');
  const [receiptReason, setReceiptReason] = useState('Incasso ordine');
  const [returnProductCode, setReturnProductCode] = useState('');
  const [returnQuantity, setReturnQuantity] = useState('1');
  const [returnReason, setReturnReason] = useState('');
  const [reviewNote, setReviewNote] = useState('');
  const [refundReturnCode, setRefundReturnCode] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReference, setRefundReference] = useState('');
  const [refundReason, setRefundReason] = useState('Rimborso reso ricevuto');

  const selectedReturn = useMemo(() => props.order?.returns.find((item) => item.code === refundReturnCode) ?? null, [props.order, refundReturnCode]);

  useEffect(() => {
    setReceiptAmount(props.order?.payment.outstandingAmount ? String(props.order.payment.outstandingAmount) : '');
    setReturnProductCode(props.order?.items[0]?.productCode ?? '');
    setRefundReturnCode('');
  }, [props.order?.code]);

  if (!props.order) {
    return <section className="panel order-operations empty-operation"><div className="section-heading compact"><span>Scheda operativa</span><h2>Seleziona un ordine</h2><p>Apri un ordine dalla tabella per gestire incassi, movimenti e resi.</p></div></section>;
  }

  const canReceivePayment = props.canRecordPayments && ['CONFIRMED', 'FULFILLED'].includes(props.order.status) && props.order.payment.outstandingAmount > 0;
  const canOpenReturn = props.canRequestReturns && props.order.status === 'FULFILLED';

  function submitReceipt(event: FormEvent) {
    event.preventDefault();
    props.onReceipt(props.order!.code, { amount: Number(receiptAmount), reference: receiptReference, reason: receiptReason });
  }

  function submitReturn(event: FormEvent) {
    event.preventDefault();
    props.onRequestReturn(props.order!.code, { reason: returnReason, items: [{ productCode: returnProductCode, quantity: Number(returnQuantity) }] });
  }

  function submitRefund(event: FormEvent) {
    event.preventDefault();
    if (!selectedReturn) return;
    props.onRefundReturn(props.order!.code, selectedReturn.code, { amount: Number(refundAmount), reference: refundReference, reason: refundReason });
  }

  return (
    <section className="panel order-operations">
      <div className="section-heading compact operation-heading">
        <span>Scheda operativa</span>
        <h2>{props.order.code}</h2>
        <p>{props.order.customer} · {props.order.statusLabel}</p>
      </div>

      <div className="payment-summary">
        <Metric label="Totale" value={money.format(props.order.payment.requestedAmount)} />
        <Metric label="Incassato" value={money.format(props.order.payment.paidAmount)} />
        <Metric label="Rimborsato" value={money.format(props.order.payment.refundedAmount)} />
        <Metric label="Residuo" value={money.format(props.order.payment.outstandingAmount)} />
      </div>

      <div className="operation-grid">
        <div className="operation-block">
          <h3>Movimenti pagamento</h3>
          <div className="operation-ledger">
            {props.order.payment.transactions.length ? props.order.payment.transactions.map((transaction) => (
              <div className="ledger-row" key={transaction.code}>
                <div><strong>{transaction.typeLabel}</strong><small>{transaction.code} · {dateTime.format(new Date(transaction.recordedAt))}</small></div>
                <span className={transaction.type === 'REFUND' ? 'negative-amount' : ''}>{transaction.type === 'REFUND' ? '-' : '+'}{money.format(transaction.amount)}</span>
              </div>
            )) : <p className="empty-copy">Nessun movimento registrato.</p>}
          </div>
          {canReceivePayment && (
            <form className="compact-operation-form" onSubmit={submitReceipt}>
              <label>Importo<input type="number" min="0.01" step="0.01" max={props.order.payment.outstandingAmount} required value={receiptAmount} onChange={(event) => setReceiptAmount(event.target.value)} /></label>
              <label>Riferimento<input maxLength={120} value={receiptReference} onChange={(event) => setReceiptReference(event.target.value)} placeholder="POS, CRO o quietanza" /></label>
              <label className="wide-field">Causale<input maxLength={500} required value={receiptReason} onChange={(event) => setReceiptReason(event.target.value)} /></label>
              <button className="button primary compact-button" disabled={props.busy}>Registra incasso</button>
            </form>
          )}
        </div>

        <div className="operation-block">
          <h3>Resi</h3>
          {canOpenReturn && (
            <form className="compact-operation-form" onSubmit={submitReturn}>
              <label>Prodotto<select required value={returnProductCode} onChange={(event) => setReturnProductCode(event.target.value)}>{props.order.items.map((item) => <option key={item.productCode} value={item.productCode}>{item.productCode} · {item.productName}</option>)}</select></label>
              <label>Quantita<input type="number" min="1" required value={returnQuantity} onChange={(event) => setReturnQuantity(event.target.value)} /></label>
              <label className="wide-field">Motivazione<input maxLength={500} required value={returnReason} onChange={(event) => setReturnReason(event.target.value)} placeholder="Motivo verificabile del reso" /></label>
              <button className="button secondary compact-button" disabled={props.busy}>Richiedi reso</button>
            </form>
          )}
          <div className="return-list">
            {props.order.returns.length ? props.order.returns.map((orderReturn) => (
              <article className="return-card" key={orderReturn.code}>
                <div className="return-card-heading"><div><strong>{orderReturn.code}</strong><small>{orderReturn.statusLabel} · {money.format(orderReturn.totalAmount)}</small></div><span>{orderReturn.items.map((item) => `${item.productCode} × ${item.quantity}`).join(', ')}</span></div>
                <p>{orderReturn.reason}</p>
                {props.canManageReturns && orderReturn.status === 'REQUESTED' && <div className="return-actions"><input value={reviewNote} onChange={(event) => setReviewNote(event.target.value)} placeholder="Nota revisione" /><button className="link-button" onClick={() => props.onApproveReturn(props.order!.code, orderReturn.code, reviewNote)}>Approva</button><button className="link-button danger-text" onClick={() => props.onRejectReturn(props.order!.code, orderReturn.code, reviewNote)}>Rifiuta</button></div>}
                {props.canManageReturns && orderReturn.status === 'APPROVED' && <button className="button secondary compact-button" onClick={() => props.onReceiveReturn(props.order!.code, orderReturn.code)}>Registra ricezione</button>}
                {props.canRefundPayments && ['RECEIVED', 'PARTIALLY_REFUNDED'].includes(orderReturn.status) && <button className="link-button" onClick={() => { setRefundReturnCode(orderReturn.code); setRefundAmount(String(Math.min(orderReturn.refundableAmount, props.order!.payment.refundableAmount))); }}>Prepara rimborso</button>}
              </article>
            )) : <p className="empty-copy">Nessun reso associato.</p>}
          </div>
          {selectedReturn && (
            <form className="compact-operation-form refund-form" onSubmit={submitRefund}>
              <strong>Rimborso {selectedReturn.code}</strong>
              <label>Importo<input type="number" min="0.01" step="0.01" max={Math.min(selectedReturn.refundableAmount, props.order.payment.refundableAmount)} required value={refundAmount} onChange={(event) => setRefundAmount(event.target.value)} /></label>
              <label>Riferimento<input maxLength={120} value={refundReference} onChange={(event) => setRefundReference(event.target.value)} /></label>
              <label className="wide-field">Causale<input maxLength={500} required value={refundReason} onChange={(event) => setRefundReason(event.target.value)} /></label>
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
