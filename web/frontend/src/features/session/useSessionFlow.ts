import { FormEvent, useRef, useState } from 'react';
import { changeOwnPassword, login, logoutSession, register, renewSession, SessionExpiredError } from '../../api';
import { useSession } from '../../session/SessionProvider';
import { AuthFormState, AuthMode } from '../../types/ui';
import { errorMessage } from '../../utils/errors';

type SessionFlowOptions = {
  onAuthenticated: () => void;
  onSignedOut: () => void;
  onNotice: (message: string, title: string) => void;
};

export default function useSessionFlow({ onAuthenticated, onSignedOut, onNotice }: SessionFlowOptions) {
  const { currentUser, sessionExpiresAt, renewalMessage, startSession, clearSession, requestSessionRenewal } = useSession();
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [authForm, setAuthForm] = useState<AuthFormState>({ username: '', password: '' });
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [authBusy, setAuthBusy] = useState(false);
  const [authMessage, setAuthMessage] = useState('');
  const [sessionPassword, setSessionPassword] = useState('');
  const [renewalBusy, setRenewalBusy] = useState(false);
  const [passwordChangeOpen, setPasswordChangeOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [passwordChangeMessage, setPasswordChangeMessage] = useState('');
  const [passwordChangeBusy, setPasswordChangeBusy] = useState(false);
  const authInFlight = useRef(false);
  const renewalInFlight = useRef(false);
  const passwordChangeInFlight = useRef(false);
  const logoutInFlight = useRef(false);

  async function submitAuthentication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (authInFlight.current) return;
    authInFlight.current = true;
    setAuthBusy(true);
    setAuthMessage('');
    try {
      if (authMode === 'register') {
        await register(authForm.username, authForm.password);
      }
      const session = await login(authForm.username, authForm.password);
      startSession(session);
      onAuthenticated();
    } catch (exception) {
      setAuthMessage(errorMessage(exception));
    } finally {
      authInFlight.current = false;
      setAuthBusy(false);
    }
  }

  async function submitRenewal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentUser || renewalInFlight.current) return;
    renewalInFlight.current = true;
    setRenewalBusy(true);
    try {
      let session;
      try {
        session = await renewSession(sessionPassword);
      } catch (exception) {
        if (!(exception instanceof SessionExpiredError)) throw exception;
        session = await login(currentUser.username, sessionPassword);
      }
      startSession(session);
      setSessionPassword('');
      onNotice('Sessione rinnovata. Puoi continuare.', 'Sessione rinnovata');
    } catch (exception) {
      requestSessionRenewal(errorMessage(exception, 'Impossibile rinnovare la sessione.'));
    } finally {
      renewalInFlight.current = false;
      setRenewalBusy(false);
    }
  }

  async function submitPasswordChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (passwordChangeInFlight.current) return;
    passwordChangeInFlight.current = true;
    setPasswordChangeBusy(true);
    setPasswordChangeMessage('');
    try {
      await changeOwnPassword(currentPassword, newPassword);
      clearSession();
      setCurrentPassword('');
      setNewPassword('');
      setPasswordChangeOpen(false);
      setAuthMessage('Password aggiornata. Accedi di nuovo con le nuove credenziali.');
      onSignedOut();
    } catch (exception) {
      setPasswordChangeMessage(errorMessage(exception, 'Impossibile cambiare la password.'));
    } finally {
      passwordChangeInFlight.current = false;
      setPasswordChangeBusy(false);
    }
  }

  async function signOut() {
    if (logoutInFlight.current) return;
    logoutInFlight.current = true;
    try {
      await logoutSession().catch(() => undefined);
    } finally {
      clearSession();
      setSessionPassword('');
      setPasswordChangeOpen(false);
      setPasswordChangeMessage('');
      onSignedOut();
      logoutInFlight.current = false;
    }
  }

  function openPasswordChange() {
    setPasswordChangeMessage('');
    setPasswordChangeOpen(true);
  }

  function closePasswordChange() {
    if (passwordChangeInFlight.current) return;
    setPasswordChangeOpen(false);
    setPasswordChangeMessage('');
  }

  return {
    currentUser,
    sessionExpiresAt,
    renewalMessage,
    authMode,
    authForm,
    passwordVisible,
    authBusy,
    authMessage,
    sessionPassword,
    renewalBusy,
    passwordChangeOpen,
    currentPassword,
    newPassword,
    passwordChangeMessage,
    passwordChangeBusy,
    setAuthMode,
    setAuthForm,
    setPasswordVisible,
    setSessionPassword,
    setCurrentPassword,
    setNewPassword,
    submitAuthentication,
    submitRenewal,
    submitPasswordChange,
    signOut,
    openPasswordChange,
    closePasswordChange
  };
}

export type SessionFlowController = ReturnType<typeof useSessionFlow>;
