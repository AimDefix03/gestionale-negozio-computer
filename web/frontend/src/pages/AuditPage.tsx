import { AuditEvent, AuditQuery, PageResponse } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import { dateTime } from '../utils/formatters';

const auditCategories = ['SECURITY', 'ACCOUNT', 'PARTNER', 'PRODUCT', 'INVENTORY', 'ORDER', 'DOCUMENT', 'SYSTEM'];

type Props = {
  page: PageResponse<AuditEvent>;
  query: AuditQuery;
  pageSize: number;
  onQueryChange: (query: AuditQuery) => void;
};

export default function AuditPage({ page, query, pageSize, onQueryChange }: Props) {
  return (
    <DataList
      title="Audit log"
      rows={page.content.map((event) => [event.severity, event.entityType, event.action, event.actor, event.target, `${event.details} · ${event.source} · ${event.requestId}`, dateTime.format(new Date(event.timestamp))])}
      footer={<PaginationControls page={page} onPageChange={(nextPage) => onQueryChange({ ...query, page: nextPage })} />}
    >
      <div className="list-filters">
        <label>Cerca<input value={query.q ?? ''} placeholder="Azione, target, richiesta o operatore" onChange={(event) => onQueryChange({ ...query, q: event.target.value, page: 0 })} /></label>
        <label>Categoria<select value={query.category ?? 'ALL'} onChange={(event) => onQueryChange({ ...query, category: event.target.value, page: 0 })}><option value="ALL">Tutte</option>{auditCategories.map((category) => <option key={category} value={category}>{category}</option>)}</select></label>
        <label>Severita<select value={query.severity ?? 'ALL'} onChange={(event) => onQueryChange({ ...query, severity: event.target.value as AuditQuery['severity'], page: 0 })}><option value="ALL">Tutte</option><option value="INFO">Info</option><option value="WARNING">Warning</option><option value="CRITICAL">Critical</option></select></label>
        <button className="button secondary compact-button" type="button" onClick={() => onQueryChange({ page: 0, size: pageSize })}>Reset</button>
      </div>
    </DataList>
  );
}
