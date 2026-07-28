import { FormEvent } from 'react';

type Props = {
  message: string;
  password: string;
  busy: boolean;
  onPasswordChange: (password: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onLogout: () => void;
};

export default function SessionRenewalModal({ message, password, busy, onPasswordChange, onSubmit, onLogout }: Props) {
  return (
    <div className="modal-backdrop" role="presentation">
      <section className="session-modal" role="dialog" aria-modal="true" aria-labelledby="session-renewal-title">
        <div className="section-heading compact">
          <span>Sessione</span>
          <h2 id="session-renewal-title">Riconferma accesso</h2>
          <p>La schermata resta aperta. Inserisci la password per rinnovare la sessione e continuare.</p>
        </div>
        {message && <div className="alert" role="alert">{message}</div>}
        <form className="form-grid single" onSubmit={onSubmit}>
          <label>Password<input type="password" value={password} onChange={(event) => onPasswordChange(event.target.value)} autoComplete="current-password" autoFocus required /></label>
          <div className="form-actions">
            <button className="button secondary" type="button" onClick={onLogout}>Esci</button>
            <button className="button primary" type="submit" disabled={busy}>{busy ? 'Verifica...' : 'Rinnova sessione'}</button>
          </div>
        </form>
      </section>
    </div>
  );
}
