import { act, renderHook } from '@testing-library/react';
import { FormEvent, ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { changeOwnPassword, login, logoutSession, register, renewSession, SessionExpiredError } from '../../api';
import { SessionProvider } from '../../session/SessionProvider';
import useSessionFlow from './useSessionFlow';

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api')>();
  return {
    ...actual,
    changeOwnPassword: vi.fn(),
    login: vi.fn(),
    logoutSession: vi.fn(),
    register: vi.fn(),
    renewSession: vi.fn()
  };
});

const account = {
  id: 1,
  username: 'superadmin',
  role: 'SUPER_ADMIN' as const,
  roleLabel: 'Super admin',
  permissions: [],
  enabled: true,
  disabledAt: null,
  disabledBy: null,
  disabledReason: null
};
const session = { user: account, token: 'session-token', expiresAt: new Date(Date.now() + 3600000).toISOString() };
const loginMock = vi.mocked(login);
const registerMock = vi.mocked(register);
const renewSessionMock = vi.mocked(renewSession);
const logoutSessionMock = vi.mocked(logoutSession);
const changeOwnPasswordMock = vi.mocked(changeOwnPassword);

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useSessionFlow', () => {
  it('autentica una sola volta anche con due submit concorrenti', async () => {
    let resolveLogin: (value: typeof session) => void = () => undefined;
    loginMock.mockImplementation(() => new Promise((resolve) => { resolveLogin = resolve; }));
    const onAuthenticated = vi.fn();
    const { result } = renderFlow({ onAuthenticated });

    act(() => result.current.setAuthForm({ username: 'superadmin', password: 'Secret123!' }));
    let first: Promise<void>;
    let second: Promise<void>;
    act(() => {
      first = result.current.submitAuthentication(submitEvent());
      second = result.current.submitAuthentication(submitEvent());
    });

    expect(loginMock).toHaveBeenCalledOnce();
    resolveLogin(session);
    await act(async () => Promise.all([first!, second!]));
    expect(result.current.currentUser).toEqual(account);
    expect(onAuthenticated).toHaveBeenCalledOnce();
  });

  it('registra il cliente prima di effettuare il login', async () => {
    registerMock.mockResolvedValue(account);
    loginMock.mockResolvedValue(session);
    const { result } = renderFlow();

    act(() => {
      result.current.setAuthMode('register');
      result.current.setAuthForm({ username: 'cliente', password: 'Secret123!' });
    });
    await act(async () => result.current.submitAuthentication(submitEvent()));

    expect(registerMock).toHaveBeenCalledWith('cliente', 'Secret123!');
    expect(loginMock).toHaveBeenCalledWith('cliente', 'Secret123!');
    expect(registerMock.mock.invocationCallOrder[0]).toBeLessThan(loginMock.mock.invocationCallOrder[0]);
  });

  it('mantiene l errore di rete nel perimetro della pagina di accesso', async () => {
    loginMock.mockRejectedValue(new Error('Connessione non disponibile.'));
    const { result } = renderFlow();

    act(() => result.current.setAuthForm({ username: 'utente', password: 'Secret123!' }));
    await act(async () => result.current.submitAuthentication(submitEvent()));

    expect(result.current.currentUser).toBeNull();
    expect(result.current.authMessage).toBe('Connessione non disponibile.');
  });

  it('rinnova con login controllato quando il token e gia scaduto', async () => {
    loginMock.mockResolvedValue(session);
    const onNotice = vi.fn();
    const { result } = renderFlow({ onNotice });
    act(() => result.current.setAuthForm({ username: 'superadmin', password: 'Secret123!' }));
    await act(async () => result.current.submitAuthentication(submitEvent()));
    renewSessionMock.mockRejectedValue(new SessionExpiredError('Token scaduto.'));
    loginMock.mockResolvedValue({ ...session, token: 'rotated-token' });

    act(() => result.current.setSessionPassword('Secret123!'));
    await act(async () => result.current.submitRenewal(submitEvent()));

    expect(renewSessionMock).toHaveBeenCalledWith('Secret123!');
    expect(loginMock).toHaveBeenLastCalledWith('superadmin', 'Secret123!');
    expect(onNotice).toHaveBeenCalledWith('Sessione rinnovata. Puoi continuare.', 'Sessione rinnovata');
    expect(result.current.sessionPassword).toBe('');
  });

  it('cambia la password, chiude la sessione e conserva il messaggio di rientro', async () => {
    loginMock.mockResolvedValue(session);
    changeOwnPasswordMock.mockResolvedValue(undefined);
    const onSignedOut = vi.fn();
    const { result } = renderFlow({ onSignedOut });
    act(() => result.current.setAuthForm({ username: 'superadmin', password: 'Secret123!' }));
    await act(async () => result.current.submitAuthentication(submitEvent()));

    act(() => {
      result.current.setCurrentPassword('Secret123!');
      result.current.setNewPassword('NewSecret456!');
    });
    await act(async () => result.current.submitPasswordChange(submitEvent()));

    expect(changeOwnPasswordMock).toHaveBeenCalledWith('Secret123!', 'NewSecret456!');
    expect(result.current.currentUser).toBeNull();
    expect(result.current.authMessage).toContain('Password aggiornata');
    expect(onSignedOut).toHaveBeenCalledOnce();
  });

  it('pulisce sempre la sessione locale quando il logout remoto fallisce', async () => {
    loginMock.mockResolvedValue(session);
    logoutSessionMock.mockRejectedValue(new Error('offline'));
    const onSignedOut = vi.fn();
    const { result } = renderFlow({ onSignedOut });
    act(() => result.current.setAuthForm({ username: 'superadmin', password: 'Secret123!' }));
    await act(async () => result.current.submitAuthentication(submitEvent()));

    await act(async () => result.current.signOut());

    expect(result.current.currentUser).toBeNull();
    expect(onSignedOut).toHaveBeenCalledOnce();
  });
});

function renderFlow(overrides: Partial<Parameters<typeof useSessionFlow>[0]> = {}) {
  return renderHook(() => useSessionFlow({
    onAuthenticated: vi.fn(),
    onSignedOut: vi.fn(),
    onNotice: vi.fn(),
    ...overrides
  }), { wrapper: Provider });
}

function Provider({ children }: { children: ReactNode }) {
  return <SessionProvider>{children}</SessionProvider>;
}

function submitEvent(): FormEvent<HTMLFormElement> {
  return { preventDefault: vi.fn() } as unknown as FormEvent<HTMLFormElement>;
}
