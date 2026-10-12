import type { FinancialReconciliation } from '../../api';
import { money } from '../../utils/formatters';

type Props = {
  reconciliation: FinancialReconciliation;
};

export default function FinancialReconciliationPanel({ reconciliation }: Props) {
  return (
    <section className="panel reconciliation-panel financial-reconciliation-panel">
      <div className="panel-toolbar">
        <div className="section-heading compact">
          <span>Controllo finanziario</span>
          <h2>Riconciliazione pagamenti e resi</h2>
          <p>Confronto tra ledger, saldi operativi e rimborsi collegati.</p>
        </div>
        <span className={`status-badge ${reconciliation.balanced ? 'ok' : 'critical'}`}>
          {reconciliation.balanced ? 'Saldi riconciliati' : `${reconciliation.mismatchCount} anomalie`}
        </span>
      </div>
      <div className="reconciliation-summary financial-reconciliation-summary">
        <div><span>Pagamenti verificati</span><strong>{reconciliation.checkedPayments}</strong></div>
        <div><span>Resi verificati</span><strong>{reconciliation.checkedReturns}</strong></div>
        <div><span>Anomalie</span><strong>{reconciliation.mismatchCount}</strong></div>
      </div>
      {reconciliation.balanced ? (
        <p className="reconciliation-ok">Ledger finanziario e proiezioni operative coincidono.</p>
      ) : (
        <div className="reconciliation-list">
          {reconciliation.mismatches.slice(0, 12).map((mismatch) => (
            <div key={`${mismatch.type}-${mismatch.aggregateType}-${mismatch.aggregateId}`}>
              <span><strong>{mismatch.aggregateCode}</strong><small>Ordine {mismatch.orderCode} · {mismatch.aggregateType === 'PAYMENT' ? 'Pagamento' : 'Reso'}</small></span>
              <span>{mismatch.detail}<small>{amounts(mismatch.materializedAmount, mismatch.ledgerAmount)}</small></span>
              <span className="status-badge critical">Da verificare</span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function amounts(materializedAmount: number | null, ledgerAmount: number | null): string {
  if (materializedAmount === null || ledgerAmount === null) return 'Controllo strutturale';
  return `Proiezione ${money.format(materializedAmount)} · Ledger ${money.format(ledgerAmount)}`;
}
