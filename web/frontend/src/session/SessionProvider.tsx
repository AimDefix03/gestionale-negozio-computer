import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { AuthSession, clearSessionToken, SessionExpiredError, setSessionToken, subscribeToSessionExpiration, UserAccount } from '../api';

type SessionContextValue = {
  currentUser: UserAccount | null;
  sessionExpiresAt: string;
  renewalMessage: string | null;
  startSession: (session: AuthSession) => void;
  clearSession: () => void;
  requestSessionRenewal: (message: string) => void;
};

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<UserAccount | null>(null);
  const [sessionExpiresAt, setSessionExpiresAt] = useState('');
  const [renewalMessage, setRenewalMessage] = useState<string | null>(null);

  const startSession = useCallback((session: AuthSession) => {
    setSessionToken(session.token);
    setCurrentUser(session.user);
    setSessionExpiresAt(session.expiresAt);
    setRenewalMessage(null);
  }, []);

  const clearSession = useCallback(() => {
    clearSessionToken();
    setCurrentUser(null);
    setSessionExpiresAt('');
    setRenewalMessage(null);
  }, []);

  const requestSessionRenewal = useCallback((message: string) => {
    if (currentUser) setRenewalMessage(message);
  }, [currentUser]);

  useEffect(() => subscribeToSessionExpiration((error) => {
    requestSessionRenewal(formatSessionError(error));
  }), [requestSessionRenewal]);

  useEffect(() => {
    if (!currentUser || !sessionExpiresAt) return;

    const checkSession = () => {
      const remaining = new Date(sessionExpiresAt).getTime() - Date.now();
      if (remaining <= 0) {
        setRenewalMessage('Sessione scaduta. Riconferma la password per continuare.');
      } else if (remaining <= 3 * 60 * 1000) {
        setRenewalMessage('La sessione sta per scadere. Rinnovala prima di continuare con operazioni delicate.');
      }
    };

    checkSession();
    const interval = window.setInterval(checkSession, 30000);
    return () => window.clearInterval(interval);
  }, [currentUser, sessionExpiresAt]);

  const value = useMemo<SessionContextValue>(() => ({
    currentUser,
    sessionExpiresAt,
    renewalMessage,
    startSession,
    clearSession,
    requestSessionRenewal
  }), [clearSession, currentUser, renewalMessage, requestSessionRenewal, sessionExpiresAt, startSession]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext);
  if (!context) throw new Error('useSession deve essere usato dentro SessionProvider.');
  return context;
}

function formatSessionError(error: SessionExpiredError): string {
  return error.requestId ? `${error.message} Codice richiesta: ${error.requestId}` : error.message;
}
