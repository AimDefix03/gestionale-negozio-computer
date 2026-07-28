import { FormEvent } from 'react';
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
  onQueryChange: (query: AccountQuery) => void;
  onFormChange: (form: AccountFormState) => void;
  onReauthPasswordChange: (password: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  canDelete: (account: UserAccount) => boolean;
  protectionLabel: (account: UserAccount) => string;
  onDelete: (username: string) => void;
};

export default function AccountsPage(props: Props) {
  return (
    <div className="content-grid two">
      <section className="panel">
        <div className="section-heading compact"><span>Account</span><h2>Crea account</h2><p>Gli account admin possono essere creati solo dal super admin.</p></div>
        <form className="form-grid single" onSubmit={props.onSubmit}>
          <label>Username<input value={props.form.username} onChange={(event) => props.onFormChange({ ...props.form, username: event.target.value })} autoComplete="off" required /></label>
          <label>Password<input type="password" value={props.form.password} onChange={(event) => props.onFormChange({ ...props.form, password: event.target.value })} autoComplete="new-password" required /></label>
          <label>Ruolo<select value={props.form.role} onChange={(event) => props.onFormChange({ ...props.form, role: event.target.value as UserRole })}>{props.isSuperAdmin && <option value="ADMIN">Admin</option>}<option value="EMPLOYEE">Dipendente</option><option value="CUSTOMER">Cliente</option></select></label>
          <label>Password sessione<input type="password" value={props.reauthPassword} onChange={(event) => props.onReauthPasswordChange(event.target.value)} autoComplete="current-password" required /></label>
          <p className="security-note">Richiesta per creazione ed eliminazione account.</p>
          <button className="button primary" disabled={props.busy}>Crea account</button>
        </form>
      </section>
      <DataList
        title="Utenti creati"
        rows={props.page.content.map((account) => [account.username, account.roleLabel])}
        actions={(username) => {
          const account = props.page.content.find((item) => item.username === username);
          if (!account) return null;
          return props.canDelete(account) ? <button className="link-button danger-text" onClick={() => props.onDelete(account.username)}>Elimina</button> : <span className="locked-action">{props.protectionLabel(account)}</span>;
        }}
        footer={<PaginationControls page={props.page} onPageChange={(page) => props.onQueryChange({ ...props.query, page })} />}
      >
        <div className="section-copy">Gestione eliminazione account e ruoli protetti.</div>
        <div className="list-filters">
          <label>Cerca<input value={props.query.q ?? ''} placeholder="Username" onChange={(event) => props.onQueryChange({ ...props.query, q: event.target.value, page: 0 })} /></label>
          <label>Ruolo<select value={props.query.role ?? 'ALL'} onChange={(event) => props.onQueryChange({ ...props.query, role: event.target.value as AccountQuery['role'], page: 0 })}><option value="ALL">Tutti</option><option value="SUPER_ADMIN">Super admin</option><option value="ADMIN">Admin</option><option value="EMPLOYEE">Dipendente</option><option value="CUSTOMER">Cliente</option></select></label>
          <button className="button secondary compact-button" type="button" onClick={() => props.onQueryChange({ page: 0, size: props.pageSize })}>Reset</button>
        </div>
      </DataList>
    </div>
  );
}
