import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormEvent, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import SessionRenewalModal from './SessionRenewalModal';

function RenewalHarness({ onSubmit, onLogout }: { onSubmit: (event: FormEvent<HTMLFormElement>) => void; onLogout: () => void }) {
  const [password, setPassword] = useState('');
  return (
    <SessionRenewalModal
      message="La sessione sta per scadere."
      password={password}
      busy={false}
      onPasswordChange={setPassword}
      onSubmit={onSubmit}
      onLogout={onLogout}
    />
  );
}

describe('SessionRenewalModal', () => {
  it('mantiene il contesto e inoltra la nuova password', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn((event) => event.preventDefault());
    render(<RenewalHarness onSubmit={onSubmit} onLogout={vi.fn()} />);

    expect(screen.getByRole('dialog', { name: 'Riconferma accesso' })).toBeInTheDocument();
    expect(screen.getByText('La sessione sta per scadere.')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Password'), 'Secret123!');
    await user.click(screen.getByRole('button', { name: 'Rinnova sessione' }));

    expect(screen.getByLabelText('Password')).toHaveValue('Secret123!');
    expect(onSubmit).toHaveBeenCalledOnce();
  });

  it('consente l uscita e blocca il rinnovo durante la verifica', async () => {
    const user = userEvent.setup();
    const onLogout = vi.fn();
    render(
      <SessionRenewalModal
        message=""
        password="secret"
        busy
        onPasswordChange={vi.fn()}
        onSubmit={vi.fn()}
        onLogout={onLogout}
      />
    );

    expect(screen.getByRole('button', { name: 'Verifica...' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Esci' }));
    expect(onLogout).toHaveBeenCalledOnce();
  });
});
