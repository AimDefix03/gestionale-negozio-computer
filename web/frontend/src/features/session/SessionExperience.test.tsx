import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { login } from '../../api';
import { SessionProvider, useSession } from '../../session/SessionProvider';
import { SessionEntry, SessionOverlays } from './SessionExperience';
import useSessionFlow from './useSessionFlow';

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api')>();
  return { ...actual, login: vi.fn() };
});

const loginMock = vi.mocked(login);

describe('SessionExperience', () => {
  it('collega pagina di accesso e rinnovo senza stato sessione in App', async () => {
    const user = userEvent.setup();
    loginMock.mockResolvedValue({
      user: {
        id: 1,
        username: 'superadmin',
        role: 'SUPER_ADMIN',
        roleLabel: 'Super admin',
        permissions: [],
        enabled: true,
        disabledAt: null,
        disabledBy: null,
        disabledReason: null
      },
      token: 'session-token',
      expiresAt: new Date(Date.now() + 3600000).toISOString()
    });
    render(<SessionProvider><Harness /></SessionProvider>);

    await user.type(screen.getByLabelText('Username'), 'superadmin');
    await user.type(screen.getByLabelText('Password'), 'Secret123!');
    await user.click(screen.getByRole('button', { name: 'Accedi' }));
    expect(await screen.findByText('Workspace autenticato')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Richiedi rinnovo' }));
    expect(screen.getByRole('dialog', { name: 'Riconferma accesso' })).toBeInTheDocument();
  });
});

function Harness() {
  const provider = useSession();
  const flow = useSessionFlow({ onAuthenticated: vi.fn(), onSignedOut: vi.fn(), onNotice: vi.fn() });
  if (!flow.currentUser) return <SessionEntry flow={flow} />;
  return (
    <>
      <p>Workspace autenticato</p>
      <button onClick={() => provider.requestSessionRenewal('Rinnovo richiesto.')}>Richiedi rinnovo</button>
      <SessionOverlays flow={flow} />
    </>
  );
}
