import { FormEvent, useRef } from 'react';
import AccessibleDialog from '../common/AccessibleDialog';

type Props = {
  message: string;
  password: string;
  busy: boolean;
  onPasswordChange: (password: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onLogout: () => void;
};

export default function SessionRenewalModal({ message, password, busy, onPasswordChange, onSubmit, onLogout }: Props) {
  const passwordRef = useRef<HTMLInputElement>(null);

  return (
    <AccessibleDialog
      className="session-modal"
      labelledBy="session-renewal-title"
      describedBy="session-renewal-description"
      initialFocusRef={passwordRef}
    >
        <div className="section-heading compact">
          <span>Sessione</span>
          <h2 id="session-renewal-title">Riconferma accesso</h2>
          <p id="session-renewal-description">La schermata resta aperta. Inserisci la password per rinnovare la sessione e continuare.</p>
        </div>
        {message && <div className="alert" role="alert">{message}</div>}
        <form className="form-grid single" onSubmit={onSubmit}>
          <label>Password<input ref={passwordRef} type="password" value={password} onChange={(event) => onPasswordChange(event.target.value)} autoComplete="current-password" required /></label>
          <div className="form-actions">
            <button className="button secondary" type="button" onClick={onLogout}>Esci</button>
            <button className="button primary" type="submit" disabled={busy}>{busy ? 'Verifica...' : 'Rinnova sessione'}</button>
          </div>
        </form>
    </AccessibleDialog>
  );
}
