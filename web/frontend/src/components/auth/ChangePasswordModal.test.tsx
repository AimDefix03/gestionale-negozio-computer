import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormEvent, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import ChangePasswordModal from './ChangePasswordModal';

function Harness({ onSubmit = (event) => event.preventDefault() }: { onSubmit?: (event: FormEvent<HTMLFormElement>) => void }) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  return <ChangePasswordModal currentPassword={currentPassword} newPassword={newPassword} message="" busy={false} onCurrentPasswordChange={setCurrentPassword} onNewPasswordChange={setNewPassword} onSubmit={onSubmit} onClose={vi.fn()} />;
}

describe('ChangePasswordModal', () => {
  it('richiede password corrente e nuova e comunica la revoca delle sessioni', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn((event) => event.preventDefault());
    render(<Harness onSubmit={onSubmit} />);

    expect(screen.getByText('La modifica chiude tutte le sessioni, inclusa quella corrente.')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Password corrente'), 'CurrentPassword123!');
    await user.type(screen.getByLabelText('Nuova password'), 'NewPassword123!');
    await user.click(screen.getByRole('button', { name: 'Cambia password' }));

    expect(onSubmit).toHaveBeenCalledOnce();
  });
});
