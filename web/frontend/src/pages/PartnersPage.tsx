import { FormEvent } from 'react';
import { BusinessPartner, BusinessPartnerPayload, BusinessPartnerType, PageResponse, PartnerQuery } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';

type Props = {
  page: PageResponse<BusinessPartner>;
  query: PartnerQuery;
  form: BusinessPartnerPayload;
  editingPartner: BusinessPartner | null;
  canManage: boolean;
  busy: boolean;
  pageSize: number;
  onQueryChange: (query: PartnerQuery) => void;
  onFormChange: (form: BusinessPartnerPayload) => void;
  onEdit: (partner: BusinessPartner) => void;
  onDeactivate: (code: string) => void;
  onCancelEdit: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function PartnersPage({ page, query, form, editingPartner, canManage, busy, pageSize, onQueryChange, onFormChange, onEdit, onDeactivate, onCancelEdit, onSubmit }: Props) {
  return (
    <div className="content-grid two">
      <DataList
        title="Anagrafiche"
        rows={page.content.map((partner) => [
          partner.code,
          partner.displayName,
          partner.typeLabel,
          partner.email || partner.phone || '-',
          partner.city || '-',
          <span className={`status-badge ${partner.active ? 'ok' : 'out'}`}>{partner.active ? 'Attiva' : 'Disattivata'}</span>
        ])}
        actions={(code) => {
          const partner = page.content.find((item) => item.code === code);
          if (!partner || !canManage) return null;
          return <div className="row-actions"><button className="link-button" onClick={() => onEdit(partner)}>Modifica</button>{partner.active && <button className="link-button danger-text" onClick={() => onDeactivate(partner.code)}>Disattiva</button>}</div>;
        }}
        footer={<PaginationControls page={page} onPageChange={(nextPage) => onQueryChange({ ...query, page: nextPage })} />}
      >
        <div className="list-filters">
          <label>Cerca<input value={query.q ?? ''} placeholder="Codice, nome, email, citta" onChange={(event) => onQueryChange({ ...query, q: event.target.value, page: 0 })} /></label>
          <label>Tipo<select value={query.type ?? 'ALL'} onChange={(event) => onQueryChange({ ...query, type: event.target.value as PartnerQuery['type'], page: 0 })}><option value="ALL">Tutti</option><option value="CUSTOMER">Clienti</option><option value="SUPPLIER">Fornitori</option></select></label>
          <label>Stato<select value={query.active === undefined ? 'ALL' : String(query.active)} onChange={(event) => onQueryChange({ ...query, active: event.target.value === 'ALL' ? undefined : event.target.value === 'true', page: 0 })}><option value="ALL">Tutti</option><option value="true">Attivi</option><option value="false">Disattivati</option></select></label>
          <button className="button secondary compact-button" type="button" onClick={() => onQueryChange({ page: 0, size: pageSize, active: true })}>Reset</button>
        </div>
      </DataList>
      <section className="panel">
        <div className="section-heading compact"><span>Anagrafica</span><h2>{editingPartner ? 'Modifica soggetto' : 'Nuovo soggetto'}</h2><p>Clienti e fornitori strutturati per ordini e operazioni future.</p></div>
        {canManage ? (
          <form className="form-grid single" onSubmit={onSubmit}>
            <label>Codice<input value={form.code} onChange={(event) => onFormChange({ ...form, code: event.target.value })} required /></label>
            <label>Tipo<select value={form.type} onChange={(event) => onFormChange({ ...form, type: event.target.value as BusinessPartnerType })}><option value="CUSTOMER">Cliente</option><option value="SUPPLIER">Fornitore</option></select></label>
            <label>Nome / Ragione sociale<input value={form.displayName} onChange={(event) => onFormChange({ ...form, displayName: event.target.value })} required /></label>
            <label>Codice fiscale<input value={form.taxCode} onChange={(event) => onFormChange({ ...form, taxCode: event.target.value })} /></label>
            <label>Partita IVA<input value={form.vatNumber} onChange={(event) => onFormChange({ ...form, vatNumber: event.target.value })} /></label>
            <label>Email<input value={form.email} onChange={(event) => onFormChange({ ...form, email: event.target.value })} /></label>
            <label>Telefono<input value={form.phone} onChange={(event) => onFormChange({ ...form, phone: event.target.value })} /></label>
            <label>Indirizzo<input value={form.address} onChange={(event) => onFormChange({ ...form, address: event.target.value })} /></label>
            <label>Citta<input value={form.city} onChange={(event) => onFormChange({ ...form, city: event.target.value })} /></label>
            <label>Note<input value={form.notes} onChange={(event) => onFormChange({ ...form, notes: event.target.value })} /></label>
            <div className="form-actions"><button className="button secondary" type="button" onClick={onCancelEdit}>Annulla</button><button className="button primary" disabled={busy}>{editingPartner ? 'Salva modifiche' : 'Crea anagrafica'}</button></div>
          </form>
        ) : <div className="empty-state">Permessi insufficienti per modificare le anagrafiche.</div>}
      </section>
    </div>
  );
}
