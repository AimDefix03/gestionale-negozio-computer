import { FormEvent, useMemo } from 'react';
import { CompanySettings, CompanySettingsPayload } from '../api';
import useDraftState from '../hooks/useDraftState';
import { dateTime } from '../utils/formatters';

type Props = {
  settings: CompanySettings | null;
  busy: boolean;
  onSave: (payload: CompanySettingsPayload) => Promise<boolean>;
  onReload: () => void;
};

export default function CompanySettingsPage({ settings, busy, onSave, onReload }: Props) {
  const initialForm = useMemo<CompanySettingsPayload | null>(() => {
    if (!settings) return null;
    const { configured: _configured, missingDocumentFields: _missingDocumentFields, updatedAt: _updatedAt, updatedBy: _updatedBy, ...payload } = settings;
    return payload;
  }, [settings]);
  const { value: form, setValue: setForm, dirty, clear } = useDraftState({
    key: 'company:settings',
    view: 'company',
    label: 'Configurazione aziendale',
    initialValue: initialForm,
    enabled: Boolean(settings)
  });

  if (!settings || !form) {
    return (
      <section className="panel">
        <div className="section-heading compact"><span>Configurazione</span><h2>Dati aziendali</h2><p>Carica la configurazione protetta del gestionale.</p></div>
        <button className="button primary compact-button" type="button" onClick={onReload}>Carica configurazione</button>
      </section>
    );
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (form && await onSave(form)) clear();
  }

  function restoreServerValues() {
    clear();
    onReload();
  }

  return (
    <form className="company-settings" onSubmit={submit}>
      <section className="panel settings-overview">
        <div className="section-heading compact"><span>Profilo azienda</span><h2>Identita operativa</h2><p>I dati vengono copiati nei nuovi documenti per conservarne lo storico.</p></div>
        <div className="settings-status">
          <span className={`status-badge ${settings.configured ? 'ok' : 'warning'}`}>{settings.configured ? 'Configurata' : 'Da completare'}</span>
          <small>Versione {settings.version} · aggiornata da {settings.updatedBy} il {dateTime.format(new Date(settings.updatedAt))}</small>
          {!settings.configured && <small>Dati necessari ai documenti mancanti: {settings.missingDocumentFields.map(documentFieldLabel).join(', ')}.</small>}
        </div>
      </section>

      <div className="settings-grid">
        <section className="panel settings-section">
          <div className="section-heading compact"><span>Anagrafica</span><h2>Azienda e contatti</h2><p>Inserisci esclusivamente i dati reali dell'organizzazione.</p></div>
          <div className="form-grid">
            <label className="wide">Ragione sociale<input value={form.legalName} maxLength={160} onChange={(event) => setForm({ ...form, legalName: event.target.value })} /></label>
            <label>Codice fiscale<input value={form.taxCode} maxLength={32} onChange={(event) => setForm({ ...form, taxCode: event.target.value })} /></label>
            <label>Partita IVA<input value={form.vatNumber} maxLength={32} onChange={(event) => setForm({ ...form, vatNumber: event.target.value })} /></label>
            <label>Email<input type="email" value={form.email} maxLength={160} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
            <label>Telefono<input value={form.phone} maxLength={40} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label>
            <label className="wide">Indirizzo<input value={form.address} maxLength={300} onChange={(event) => setForm({ ...form, address: event.target.value })} /></label>
            <label>CAP<input value={form.postalCode} maxLength={16} onChange={(event) => setForm({ ...form, postalCode: event.target.value })} /></label>
            <label>Citta<input value={form.city} maxLength={120} onChange={(event) => setForm({ ...form, city: event.target.value })} /></label>
            <label>Provincia<input value={form.province} maxLength={8} onChange={(event) => setForm({ ...form, province: event.target.value })} /></label>
            <label>Paese ISO<input value={form.countryCode} maxLength={2} placeholder="IT" onChange={(event) => setForm({ ...form, countryCode: event.target.value })} /></label>
            <label className="wide">Fuso orario aziendale<input list="company-time-zones" value={form.timeZone} maxLength={64} onChange={(event) => setForm({ ...form, timeZone: event.target.value })} required /><datalist id="company-time-zones"><option value="Europe/Rome" /><option value="Europe/Paris" /><option value="Europe/Berlin" /><option value="UTC" /></datalist></label>
          </div>
        </section>

        <section className="panel settings-section">
          <div className="section-heading compact"><span>Documenti</span><h2>IVA e numerazione</h2><p>Parametri applicati ai nuovi documenti simulati.</p></div>
          <div className="form-grid single settings-document-form">
            <label>Aliquota IVA predefinita %<input aria-label="Aliquota IVA predefinita %" type="number" min="0" max="100" step="0.01" value={Number((form.defaultVatRate * 100).toFixed(4))} onChange={(event) => setForm({ ...form, defaultVatRate: Number(event.target.value) / 100 })} required /></label>
            <div className="numbering-grid">
              <label>Prefisso fatture<input value={form.invoicePrefix} maxLength={8} pattern="[A-Za-z0-9]{1,8}" onChange={(event) => setForm({ ...form, invoicePrefix: event.target.value })} required /></label>
              <label>Prefisso note credito<input value={form.creditNotePrefix} maxLength={8} pattern="[A-Za-z0-9]{1,8}" onChange={(event) => setForm({ ...form, creditNotePrefix: event.target.value })} required /></label>
            </div>
            <label>Cifre progressivo<select value={form.numberPadding} onChange={(event) => setForm({ ...form, numberPadding: Number(event.target.value) })}>{[3, 4, 5, 6, 7, 8].map((value) => <option value={value} key={value}>{value} cifre</option>)}</select></label>
          </div>
          <div className="settings-preview">
            <span>Anteprima numerazione</span>
            <strong>{form.invoicePrefix.toUpperCase() || 'FS'}-{businessYear(form.timeZone)}-{String(1).padStart(form.numberPadding, '0')}</strong>
            <small>Prefissi e lunghezza non sono modificabili dopo il primo documento dell'esercizio.</small>
          </div>
          <div className="settings-warning"><strong>Ambito funzionale</strong><p>Questi documenti restano simulati e non costituiscono fatturazione elettronica o adempimento fiscale.</p></div>
        </section>
      </div>

      <section className="panel settings-actions">
        <p>Il salvataggio e tracciato nell'audit log e protetto da controllo versione.</p>
        <div className="form-actions">{dirty && <span className="draft-status" role="status">Bozza salvata per questa sessione</span>}<button className="button secondary" type="button" onClick={restoreServerValues}>Ripristina dati</button><button className="button primary" disabled={busy}>Salva configurazione</button></div>
      </section>
    </form>
  );
}

function businessYear(timeZone: string) {
  try {
    return new Intl.DateTimeFormat('en', { year: 'numeric', timeZone }).format(new Date());
  } catch {
    return '----';
  }
}

function documentFieldLabel(field: string) {
  return ({ legalName: 'ragione sociale', taxIdentifier: 'codice fiscale o partita IVA', address: 'indirizzo', postalCode: 'CAP', city: 'citta', province: 'provincia', countryCode: 'paese', timeZone: 'fuso orario' } as Record<string, string>)[field] ?? field;
}
