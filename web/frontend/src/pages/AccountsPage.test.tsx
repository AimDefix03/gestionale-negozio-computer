import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { PageResponse, UserAccount } from '../api';
import AccountsPage from './AccountsPage';

const account: UserAccount = {
  id: 7,
  username: 'dipendente',
  role: 'EMPLOYEE',
  roleLabel: 'Dipendente',
  permissions: [],
  enabled: true,
  disabledAt: null,
  disabledBy: null,
  disabledReason: null
};

const page: PageResponse<UserAccount> = {
  content: [account],
  page: 0,
  size: 8,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true
};

describe('AccountsPage', () => {
  it('gestisce il lifecycle senza offrire cancellazione fisica', async () => {
    const user = userEvent.setup();
    const onDisable = vi.fn();
    const onRevokeSessions = vi.fn();
    render(
      <AccountsPage
        page={page}
        query={{ page: 0, size: 8 }}
        form={{ username: '', password: '', role: 'EMPLOYEE' }}
        reauthPassword="AdminPassword123!"
        isSuperAdmin
        busy={false}
        pageSize={8}
        onQueryChange={vi.fn()}
        onFormChange={vi.fn()}
        onReauthPasswordChange={vi.fn()}
        onSubmit={(event) => event.preventDefault()}
        canManage={() => true}
        protectionLabel={() => ''}
        onDisable={onDisable}
        onEnable={vi.fn()}
        onResetPassword={vi.fn()}
        onRevokeSessions={onRevokeSessions}
        onRoleChange={vi.fn()}
      />
    );

    expect(screen.queryByRole('button', { name: 'Elimina' })).not.toBeInTheDocument();
    expect(screen.getByText('Attivo')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Gestisci' }));
    await user.type(screen.getByLabelText('Motivazione operativa'), 'Fine rapporto');
    await user.click(screen.getByRole('button', { name: 'Revoca sessioni' }));
    await user.click(screen.getByRole('button', { name: 'Disattiva accesso' }));

    expect(onRevokeSessions).toHaveBeenCalledWith('dipendente', 'Fine rapporto');
    expect(onDisable).toHaveBeenCalledWith('dipendente', 'Fine rapporto');
  });

  it('rende espliciti caricamento, refresh ed errore recuperabile', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const props = baseProps();
    const { rerender } = render(<AccountsPage {...props} page={{ ...page, content: [] }} loading />);

    expect(screen.getByRole('status')).toHaveTextContent('Caricamento dati');
    expect(screen.queryByText('Nessun dato disponibile.')).not.toBeInTheDocument();

    rerender(<AccountsPage {...props} page={page} refreshing error="Servizio account non disponibile." onRetry={onRetry} />);
    expect(screen.getByRole('status')).toHaveTextContent('Aggiornamento in corso');
    expect(screen.getByRole('alert')).toHaveTextContent('Servizio account non disponibile.');
    expect(screen.getByText('dipendente')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Riprova' }));
    expect(onRetry).toHaveBeenCalledOnce();
  });
});

function baseProps() {
  return {
    query: { page: 0, size: 8 },
    form: { username: '', password: '', role: 'EMPLOYEE' as const },
    reauthPassword: 'AdminPassword123!',
    isSuperAdmin: true,
    busy: false,
    pageSize: 8,
    onQueryChange: vi.fn(),
    onFormChange: vi.fn(),
    onReauthPasswordChange: vi.fn(),
    onSubmit: (event: React.FormEvent<HTMLFormElement>) => event.preventDefault(),
    canManage: () => true,
    protectionLabel: () => '',
    onDisable: vi.fn(),
    onEnable: vi.fn(),
    onResetPassword: vi.fn(),
    onRevokeSessions: vi.fn(),
    onRoleChange: vi.fn()
  };
}
