import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../api';
import { SessionProvider, useSession } from './SessionProvider';

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

afterEach(() => vi.unstubAllGlobals());

describe('SessionProvider', () => {
  it('apre una sola gestione centralizzata della sessione sui 401 autenticati', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      headers: new Headers({ 'X-Request-Id': 'req-session' }),
      json: vi.fn().mockResolvedValue({ message: 'Sessione revocata.', requestId: 'req-session' })
    } as unknown as Response));
    render(<SessionProvider><Harness /></SessionProvider>);

    await user.click(screen.getByRole('button', { name: 'Avvia sessione' }));
    await user.click(screen.getByRole('button', { name: 'Richiesta protetta' }));

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Sessione revocata. Codice richiesta: req-session'));
  });

  it('non tratta un 401 pubblico come scadenza di una sessione', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      headers: new Headers(),
      json: vi.fn().mockResolvedValue({ message: 'Credenziali non valide.' })
    } as unknown as Response));
    render(<SessionProvider><Harness /></SessionProvider>);

    await user.click(screen.getByRole('button', { name: 'Richiesta protetta' }));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('segnala prima della scadenza senza attendere il tick periodico', async () => {
    render(<SessionProvider><Harness expiresAt={new Date(Date.now() + 120000).toISOString()} /></SessionProvider>);

    act(() => screen.getByRole('button', { name: 'Avvia sessione' }).click());

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('sta per scadere'));
  });
});

function Harness({ expiresAt = new Date(Date.now() + 3600000).toISOString() }: { expiresAt?: string }) {
  const session = useSession();
  return (
    <div>
      <button onClick={() => session.startSession({ user: account, token: 'token', expiresAt })}>Avvia sessione</button>
      <button onClick={() => void request('/api/protected').catch(() => undefined)}>Richiesta protetta</button>
      {session.renewalMessage && <div role="alert">{session.renewalMessage}</div>}
    </div>
  );
}
