import { DocumentQuery, FiscalDocument, PageResponse } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import { dateTime, money } from '../utils/formatters';

type Props = {
  page: PageResponse<FiscalDocument>;
  query: DocumentQuery;
  creditReason: string;
  pageSize: number;
  onQueryChange: (query: DocumentQuery) => void;
  onCreditReasonChange: (reason: string) => void;
  onCreditNote: (orderCode: string) => void;
};

export default function DocumentsPage({ page, query, creditReason, pageSize, onQueryChange, onCreditReasonChange, onCreditNote }: Props) {
  return (
    <DataList
      title="Documenti simulati"
      rows={page.content.map((document) => [
        document.code,
        <div className="product-cell"><strong>{document.typeLabel}</strong><span>{document.companySnapshotLegalName || 'Emittente non configurato'} · IVA {(document.vatRate * 100).toLocaleString('it-IT')}%</span></div>,
        document.relatedOrderCode,
        <div className="product-cell"><strong>{document.customerSnapshotName || document.customer}</strong><span>{[document.customerSnapshotCode, document.customerSnapshotVatNumber || document.customerSnapshotTaxCode, document.customerSnapshotCity].filter(Boolean).join(' · ') || 'Dati minimi ordine'}</span></div>,
        <div className="product-cell"><strong>{money.format(document.totalAmount)}</strong><span>Imponibile {money.format(document.taxableAmount)}</span></div>,
        dateTime.format(new Date(document.createdAt))
      ])}
      actions={(documentCode) => {
        const document = page.content.find((item) => item.code === documentCode);
        const hasCreditNote = Boolean(document && page.content.some((item) => item.relatedOrderCode === document.relatedOrderCode && item.type === 'SIMULATED_CREDIT_NOTE'));
        if (!document || document.type !== 'SIMULATED_INVOICE' || hasCreditNote) return <span className="locked-action">Nessuna azione</span>;
        return <button className="link-button" onClick={() => onCreditNote(document.relatedOrderCode)}>Nota credito</button>;
      }}
      footer={<PaginationControls page={page} onPageChange={(nextPage) => onQueryChange({ ...query, page: nextPage })} />}
    >
      <div className="list-filters">
        <label>Cerca<input value={query.q ?? ''} placeholder="Codice, ordine, cliente o operatore" onChange={(event) => onQueryChange({ ...query, q: event.target.value, page: 0 })} /></label>
        <label>Tipo<select value={query.type ?? 'ALL'} onChange={(event) => onQueryChange({ ...query, type: event.target.value as DocumentQuery['type'], page: 0 })}><option value="ALL">Tutti</option><option value="SIMULATED_INVOICE">Fatture simulate</option><option value="SIMULATED_CREDIT_NOTE">Note credito simulate</option></select></label>
        <label>Motivo nota credito<input value={creditReason} onChange={(event) => onCreditReasonChange(event.target.value)} /></label>
        <button className="button secondary compact-button" type="button" onClick={() => onQueryChange({ page: 0, size: pageSize })}>Reset</button>
      </div>
    </DataList>
  );
}
