import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormEvent, useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { evaluatePassword } from '../api';
import type { AuthFormState, AuthMode } from '../types/ui';
import AuthPage from './AuthPage';

vi.mock('../api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api')>();
  return { ...actual, evaluatePassword: vi.fn() };
});

const evaluatePasswordMock = vi.mocked(evaluatePassword);

type HarnessProps = {
  initialMode?: AuthMode;
  initialForm?: AuthFormState;
  message?: string;
  onSubmit?: (event: FormEvent<HTMLFormElement>) => void;
};

function AuthHarness({
  initialMode = 'login',
  initialForm = { username: '', password: '' },
  message = '',
  onSubmit = (event) => event.preventDefault()
}: HarnessProps) {
  const [mode, setMode] = useState(initialMode);
  const [form, setForm] = useState(initialForm);
  const [passwordVisible, setPasswordVisible] = useState(false);

  return (
    <AuthPage
      mode={mode}
      form={form}
      passwordVisible={passwordVisible}
      busy={false}
      message={message}
      onModeChange={setMode}
      onFormChange={setForm}
      onPasswordVisibilityChange={setPasswordVisible}
      onSubmit={onSubmit}
    />
  );
}

afterEach(() => {
  vi.useRealTimers();
});

describe('AuthPage', () => {
  it('non valuta la robustezza password durante il login', async () => {
    const user = userEvent.setup();
    render(<AuthHarness />);

    await user.type(screen.getByLabelText('Password'), 'LoginPassword123!');

    expect(evaluatePasswordMock).not.toHaveBeenCalled();
    expect(screen.queryByText(/Password: /)).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Ruolo')).not.toBeInTheDocument();
  });

  it('presenta la registrazione come profilo cliente senza scelta del ruolo', async () => {
    vi.useFakeTimers();
    evaluatePasswordMock.mockResolvedValue({ strength: 'STRONG', label: 'Forte', suggestions: [] });
    render(<AuthHarness initialMode="register" initialForm={{ username: 'mario', password: 'SecurePassword123!' }} />);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(200);
    });

    expect(screen.getByText('Password: Forte')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Crea il tuo profilo cliente' })).toBeInTheDocument();
    expect(screen.getByText('La registrazione pubblica crea esclusivamente un account cliente.')).toBeInTheDocument();
    expect(screen.queryByLabelText('Ruolo')).not.toBeInTheDocument();
  });

  it('non delega mai al browser la scelta del ruolo', async () => {
    const user = userEvent.setup();
    render(<AuthHarness />);

    expect(screen.queryByLabelText('Ruolo')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Registrazione' }));
    expect(screen.queryByLabelText('Ruolo')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Login' }));
    expect(screen.queryByLabelText('Ruolo')).not.toBeInTheDocument();
  });

  it('permette di mostrare e nascondere la password', async () => {
    const user = userEvent.setup();
    render(<AuthHarness />);
    const password = screen.getByLabelText('Password');

    expect(password).toHaveAttribute('type', 'password');
    await user.click(screen.getByRole('button', { name: 'Mostra' }));
    expect(password).toHaveAttribute('type', 'text');
    await user.click(screen.getByRole('button', { name: 'Nascondi' }));
    expect(password).toHaveAttribute('type', 'password');
  });

  it('rende visibile un errore di password restituito dal backend', () => {
    render(<AuthHarness initialMode="register" message="La password non rispetta i requisiti di sicurezza." />);

    expect(screen.getByRole('alert')).toHaveTextContent('La password non rispetta i requisiti di sicurezza.');
  });
});
