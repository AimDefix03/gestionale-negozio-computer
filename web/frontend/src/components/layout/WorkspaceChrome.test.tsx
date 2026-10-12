import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { UserAccount } from '../../api';
import WorkspaceChrome from './WorkspaceChrome';

const customer: UserAccount = {
  id: 1,
  username: 'cliente_test',
  role: 'CUSTOMER',
  roleLabel: 'Cliente',
  enabled: true,
  permissions: [],
  disabledAt: null,
  disabledBy: null,
  disabledReason: null
};

describe('WorkspaceChrome', () => {
  it('presenta la vendita come nuovo ordine nella sessione cliente', () => {
    render(
      <WorkspaceChrome
        currentUser={customer}
        sessionExpiresAt=""
        menuGroups={[]}
        openMenu={null}
        openTabs={['dashboard', 'sales']}
        dirtyViews={[]}
        activeView="sales"
        notice={null}
        onMenuChange={vi.fn()}
        onOpenTab={() => true}
        onCloseTab={vi.fn()}
        onActiveViewChange={vi.fn()}
        onRefresh={vi.fn()}
        onLogout={vi.fn()}
      >
        <div>Contenuto ordine</div>
      </WorkspaceChrome>
    );

    expect(screen.getByRole('heading', { name: 'Nuovo ordine' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nuovo ordine' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('button', { name: 'Chiudi Nuovo ordine' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Nuovo ordine' })).toBeInTheDocument();
  });
});
