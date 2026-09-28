import { BusinessPartner, PageResponse, PartnerQuery } from '../../api';
import { SalesCustomerMode } from '../../types/ui';

type Props = {
  selfServiceUsername?: string;
  mode: SalesCustomerMode;
  page: PageResponse<BusinessPartner>;
  query: PartnerQuery;
  selectedPartnerId?: number;
  walkInCustomerName: string;
  busy: boolean;
  onModeChange: (mode: SalesCustomerMode) => void;
  onQueryChange: (query: PartnerQuery) => void;
  onPartnerSelect: (partner: BusinessPartner) => void;
  onWalkInCustomerNameChange: (name: string) => void;
};

export default function SalesCustomerPanel(props: Props) {
  if (props.selfServiceUsername) {
    return (
      <section className="panel sales-customer-panel">
        <div className="section-heading compact"><span>Cliente</span><h2>Ordine personale</h2><p>L'ordine sara associato esclusivamente al tuo account.</p></div>
        <div className="selected-customer-card"><strong>{props.selfServiceUsername}</strong><span>Account autenticato</span></div>
      </section>
    );
  }

  return (
    <section className="panel sales-customer-panel">
      <div className="section-heading compact"><span>Cliente</span><h2>Destinatario vendita</h2><p>Usa un'anagrafica attiva oppure registra la vendita come occasionale.</p></div>
      <div className="sales-mode-switch" role="group" aria-label="Tipo cliente">
        <button type="button" className={props.mode === 'REGISTERED' ? 'active' : ''} onClick={() => props.onModeChange('REGISTERED')}>Cliente censito</button>
        <button type="button" className={props.mode === 'WALK_IN' ? 'active' : ''} onClick={() => props.onModeChange('WALK_IN')}>Cliente occasionale</button>
      </div>
      {props.mode === 'REGISTERED' ? (
        <>
          <label className="sales-customer-search">Cerca cliente
            <input value={props.query.q ?? ''} disabled={props.busy} placeholder="Nome, codice, email o citta" onChange={(event) => props.onQueryChange({ ...props.query, q: event.target.value, page: 0 })} />
          </label>
          <div className="customer-result-list">
            {props.page.content.map((partner) => (
              <button type="button" key={partner.id} className={props.selectedPartnerId === partner.id ? 'selected' : ''} onClick={() => props.onPartnerSelect(partner)}>
                <strong>{partner.displayName}</strong><span>{partner.code}{partner.city ? ` · ${partner.city}` : ''}</span>
              </button>
            ))}
            {props.page.content.length === 0 && <div className="empty-state compact-empty">Nessun cliente attivo trovato.</div>}
          </div>
        </>
      ) : (
        <label className="sales-customer-search">Nominativo cliente occasionale
          <input value={props.walkInCustomerName} maxLength={255} disabled={props.busy} placeholder="Nome e cognome o denominazione" onChange={(event) => props.onWalkInCustomerNameChange(event.target.value)} />
        </label>
      )}
    </section>
  );
}
