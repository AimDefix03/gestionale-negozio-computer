import { FormEvent, useMemo, useState } from 'react';
import { AccountQuery, PageResponse, UserAccount, UserRole } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import { AccountFormState } from '../types/ui';

type Props = {
  page: PageResponse<UserAccount>;
  query: AccountQuery;
  form: AccountFormState;
  reauthPassword: string;
  isSuperAdmin: boolean;
  busy: boolean;
  pageSize: number;
  loading?: boolean;
  refreshing?: boolean;
  error?: string;
  onRetry?: () => void;
  onQueryChange: (query: AccountQuery) => void;
  onFormChange: (form: AccountFormState) => void;
  onReauthPasswordChange: (password: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  canManage: (account: UserAccount) => boolean;
  protectionLabel: (account: UserAccount) => string;
  onDisable: (username: string, reason: string) => void;
  onEnable: (username: string, reason: string) => void;
  onResetPassword: (username: string, newPassword: string, reason: string) => void;
  onRevokeSessions: (username: string, reason: string) => void;
  onRoleChange: (username: string, role: UserRole, reason: string) => void;
};

export default function AccountsPage(props: Props) {
  const [selectedUsername, setSelectedUsername] = useState('');
  const [reason, setReason] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newRole, setNewRole] = useState<UserRole>('EMPLOYEE');
  const selected = useMemo(() => props.page.content.find((account) => account.username === selectedUsername) ?? null, [props.page.content, selectedUsername]);
  const manageable = Boolean(selected && props.canManage(selected));

  function requireReason(action: (account: UserAccount) => void) {
    if (selected && reason.trim()) action(selected);
  }

  return (
    <div className="account-workspace">
      <div className="content-grid two">
        <section className="panel">
          <div className="section-heading compact"><span>Provisioning</span><h2>Crea account</h2><p>Gli account admin possono essere creati solo dal super admin.</p></div>
          <form className="form-grid single" onSubmit={props.onSubmit}>
            <label>Username<input value={props.form.username} onChange={(event) => props.onFormChange({ ...props.form, username: event.target.value })} autoComplete="off" required /></label>
            <label>Password iniziale<input type="password" value={props.form.password} onChange={(event) => props.onFormChange({ ...props.form, password: event.target.value })} autoComplete="new-password" required /></label>
            <label>Ruolo<select value={props.form.role} onChange={(event) => props.onFormChange({ ...props.form, role: event.target.value as UserRole })}>{props.isSuperAdmin && <option value="ADMIN">Admin</option>}<option value="EMPLOYEE">Dipendente</option><option value="CUSTOMER">Cliente</option></select></label>
            <label>Password operatore<input type="password" value={props.reauthPassword} onChange={(event) => props.onReauthPasswordChange(event.target.value)} autoComplete="current-password" required /></label>
            <p className="security-note">La ri-autenticazione protegge tutte le operazioni amministrative sugli account.</p>
            <button className="button primary" disabled={props.busy}>Crea account</button>
          </form>
        </section>
        <DataList
          title="Account conservati"
          columns={['Username', 'Ruolo', 'Stato']}
          loading={props.loading}
          refreshing={props.refreshing}
          error={props.error}
          onRetry={props.onRetry}
          rows={props.page.content.map((account) => [account.username, account.roleLabel, <span className={`status-badge ${account.enabled ? 'ok' : 'warning'}`}>{account.enabled ? 'Attivo' : 'Disattivato'}</span>])}
          actions={(username) => {
            const account = props.page.content.find((item) => item.username === username);
            if (!account) return null;
            return props.canManage(account)
              ? <button className="link-button" disabled={props.busy} onClick={() => { setSelectedUsername(account.username); setNewRole(account.role); }}>Gestisci</button>
              : <span className="locked-action">{props.protectionLabel(account)}</span>;
          }}
          footer={<PaginationControls page={props.page} onPageChange={(page) => props.onQueryChange({ ...props.query, page })} />}
        >
          <div className="section-copy">Gli account non vengono cancellati: disattivazione, sessioni e credenziali restano gestibili senza perdere lo storico.</div>
          <div className="list-filters account-filters">
            <label>Cerca<input value={props.query.q ?? ''} placeholder="Username" onChange={(event) => props.onQueryChange({ ...props.query, q: event.target.value, page: 0 })} /></label>
            <label>Ruolo<select value={props.query.role ?? 'ALL'} onChange={(event) => props.onQueryChange({ ...props.query, role: event.target.value as AccountQuery['role'], page: 0 })}><option value="ALL">Tutti</option><option value="SUPER_ADMIN">Super admin</option><option value="ADMIN">Admin</option><option value="EMPLOYEE">Dipendente</option><option value="CUSTOMER">Cliente</option></select></label>
            <label>Stato<select value={props.query.enabled === undefined ? 'ALL' : String(props.query.enabled)} onChange={(event) => props.onQueryChange({ ...props.query, enabled: event.target.value === 'ALL' ? undefined : event.target.value === 'true', page: 0 })}><option value="ALL">Tutti</option><option value="true">Attivi</option><option value="false">Disattivati</option></select></label>
            <button className="button secondary compact-button" type="button" onClick={() => props.onQueryChange({ page: 0, size: props.pageSize })}>Reset</button>
          </div>
        </DataList>
      </div>
      <section className="panel account-lifecycle-panel">
        <div className="section-heading compact"><span>Lifecycle</span><h2>{selected ? selected.username : 'Seleziona un account'}</h2><p>{selected ? `${selected.roleLabel} · ${selected.enabled ? 'accesso abilitato' : `disattivato da ${selected.disabledBy ?? 'operatore'}`}` : 'Apri un account dalla lista per gestire accesso, ruolo, credenziali e sessioni.'}</p></div>
        {selected && manageable ? (
          <div className="account-lifecycle-grid">
            <label>Motivazione operativa<textarea value={reason} maxLength={900} onChange={(event) => setReason(event.target.value)} required /></label>
            <label>Nuova password per reset<input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} autoComplete="new-password" /></label>
            {props.isSuperAdmin && selected.role !== 'SUPER_ADMIN' && <label>Nuovo ruolo<select value={newRole} onChange={(event) => setNewRole(event.target.value as UserRole)}><option value="ADMIN">Admin</option><option value="EMPLOYEE">Dipendente</option><option value="CUSTOMER">Cliente</option></select></label>}
            {!selected.enabled && selected.disabledReason && <div className="inline-warning"><strong>Motivo disattivazione</strong><span>{selected.disabledReason}</span></div>}
            <div className="form-actions lifecycle-actions">
              {selected.enabled
                ? <button className="button danger compact-button" type="button" disabled={props.busy || !reason.trim()} onClick={() => requireReason((account) => props.onDisable(account.username, reason.trim()))}>Disattiva accesso</button>
                : <button className="button primary compact-button" type="button" disabled={props.busy || !reason.trim()} onClick={() => requireReason((account) => props.onEnable(account.username, reason.trim()))}>Riabilita accesso</button>}
              <button className="button secondary compact-button" type="button" disabled={props.busy || !reason.trim()} onClick={() => requireReason((account) => props.onRevokeSessions(account.username, reason.trim()))}>Revoca sessioni</button>
              <button className="button secondary compact-button" type="button" disabled={props.busy || !reason.trim() || !newPassword} onClick={() => requireReason((account) => props.onResetPassword(account.username, newPassword, reason.trim()))}>Reset password</button>
              {props.isSuperAdmin && selected.role !== 'SUPER_ADMIN' && <button className="button secondary compact-button" type="button" disabled={props.busy || !reason.trim() || newRole === selected.role} onClick={() => requireReason((account) => props.onRoleChange(account.username, newRole, reason.trim()))}>Aggiorna ruolo</button>}
            </div>
          </div>
        ) : selected ? <div className="inline-warning">{props.protectionLabel(selected)}</div> : null}
      </section>
    </div>
  );
}
