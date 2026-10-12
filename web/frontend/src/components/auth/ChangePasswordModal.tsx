import { FormEvent, useRef } from 'react';
import usePasswordStrength from '../../hooks/usePasswordStrength';
import AccessibleDialog from '../common/AccessibleDialog';

type Props = {
  currentPassword: string;
  newPassword: string;
  message: string;
  busy: boolean;
  onCurrentPasswordChange: (password: string) => void;
  onNewPasswordChange: (password: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onClose: () => void;
};

export default function ChangePasswordModal(props: Props) {
  const strength = usePasswordStrength(props.newPassword, true);
  const currentPasswordRef = useRef<HTMLInputElement>(null);

  return (
    <AccessibleDialog
      labelledBy="change-password-title"
      describedBy="change-password-description"
      initialFocusRef={currentPasswordRef}
      onEscape={props.onClose}
    >
        <div className="section-heading compact">
          <span>Sicurezza account</span>
          <h2 id="change-password-title">Cambia password</h2>
          <p id="change-password-description">La modifica chiude tutte le sessioni, inclusa quella corrente.</p>
        </div>
        <form className="form-grid single" onSubmit={props.onSubmit}>
          <label>Password corrente<input ref={currentPasswordRef} type="password" value={props.currentPassword} onChange={(event) => props.onCurrentPasswordChange(event.target.value)} autoComplete="current-password" required /></label>
          <label>Nuova password<input type="password" value={props.newPassword} onChange={(event) => props.onNewPasswordChange(event.target.value)} autoComplete="new-password" required /></label>
          {strength && <p className={`strength ${strength.strength.toLowerCase()}`}>Password: {strength.label}</p>}
          {props.message && <div className="alert" role="alert">{props.message}</div>}
          <div className="form-actions">
            <button className="button secondary" type="button" onClick={props.onClose}>Annulla</button>
            <button className="button primary" disabled={props.busy}>{props.busy ? 'Aggiornamento...' : 'Cambia password'}</button>
          </div>
        </form>
    </AccessibleDialog>
  );
}
