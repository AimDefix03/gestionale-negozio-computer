import { FormEvent } from 'react';
import usePasswordStrength from '../hooks/usePasswordStrength';
import { AuthFormState, AuthMode } from '../types/ui';
import { UserRole } from '../api';

type Props = {
  mode: AuthMode;
  form: AuthFormState;
  passwordVisible: boolean;
  busy: boolean;
  message: string;
  onModeChange: (mode: AuthMode) => void;
  onFormChange: (form: AuthFormState) => void;
  onPasswordVisibilityChange: (visible: boolean) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function AuthPage({ mode, form, passwordVisible, busy, message, onModeChange, onFormChange, onPasswordVisibilityChange, onSubmit }: Props) {
  const passwordStrength = usePasswordStrength(form.password, mode === 'register');

  function selectMode(nextMode: AuthMode) {
    onModeChange(nextMode);
    if (nextMode === 'register') {
      onFormChange({ ...form, role: form.role === 'CUSTOMER' ? 'CUSTOMER' : 'EMPLOYEE' });
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <div className="brand-card clean">
          <div className="prompt-mark">&gt;_</div>
          <div><span>Gestionale web</span><strong>Negozio Computer</strong></div>
        </div>
        <form onSubmit={onSubmit} className="product-form">
          <div className="section-heading compact">
            <span>{mode === 'login' ? 'Accesso' : 'Registrazione'}</span>
            <h2>{mode === 'login' ? 'Accedi al workspace' : 'Crea un profilo operativo'}</h2>
            <p>{mode === 'login' ? 'Usa le credenziali assegnate per entrare nel workspace.' : 'La registrazione pubblica crea profili dipendente o cliente.'}</p>
          </div>
          <div className="segmented">
            <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => selectMode('login')}>Login</button>
            <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => selectMode('register')}>Registrazione</button>
          </div>
          <label>Username<input value={form.username} onChange={(event) => onFormChange({ ...form, username: event.target.value })} autoComplete="username" required /></label>
          <label>Password
            <div className="password-row">
              <input type={passwordVisible ? 'text' : 'password'} value={form.password} onChange={(event) => onFormChange({ ...form, password: event.target.value })} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required />
              <button type="button" className="button secondary" onClick={() => onPasswordVisibilityChange(!passwordVisible)}>{passwordVisible ? 'Nascondi' : 'Mostra'}</button>
            </div>
          </label>
          {passwordStrength && <p className={`strength ${passwordStrength.strength.toLowerCase()}`}>Password: {passwordStrength.label}</p>}
          <label>Ruolo
            <select value={form.role} onChange={(event) => onFormChange({ ...form, role: event.target.value as UserRole })}>
              {mode === 'login' && <option value="SUPER_ADMIN">Super admin</option>}
              {mode === 'login' && <option value="ADMIN">Admin</option>}
              <option value="EMPLOYEE">Dipendente</option>
              <option value="CUSTOMER">Cliente</option>
            </select>
          </label>
          {message && <div className="alert" role="alert">{message}</div>}
          <button className="button primary" disabled={busy}>{busy ? 'Attendi...' : mode === 'login' ? 'Accedi' : 'Registrati'}</button>
        </form>
      </section>
    </main>
  );
}
