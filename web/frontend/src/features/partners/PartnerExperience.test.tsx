import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it, vi } from 'vitest';
import type { BusinessPartner, PageResponse } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import PartnerExperience from './PartnerExperience';
import usePartnerFlow from './usePartnerFlow';
import { renderWithDrafts as render } from '../../test/renderWithDrafts';

const apiMocks = vi.hoisted(() => ({
  createPartner: vi.fn(),
  updatePartner: vi.fn(),
  deactivatePartner: vi.fn(),
  fetchAccounts: vi.fn(),
  fetchPartnerPage: vi.fn(),
  linkPartnerAccount: vi.fn(),
  unlinkPartnerAccount: vi.fn()
}));

vi.mock('../../api', () => apiMocks);

const partner: BusinessPartner = {
  id: 1,
  code: 'CLI-001',
  type: 'CUSTOMER',
  typeLabel: 'Cliente',
  displayName: 'Cliente Test',
  taxCode: '',
  vatNumber: '',
  email: 'cliente@example.test',
  phone: '',
  address: '',
  city: 'Napoli',
  notes: '',
  active: true,
  createdAt: '2026-08-18T10:00:00Z',
  updatedAt: '2026-08-18T10:00:00Z'
};

const executeCommand: CommandExecutor = async <T,>(options: CommandExecutionOptions<T>) => {
  const value = await options.command();
  options.applyResponse(value);
  options.afterConfirmed?.(value);
  return { status: 'saved', value };
};

it('compone elenco e form senza stato Partner in App', async () => {
  const user = userEvent.setup();
  apiMocks.fetchPartnerPage.mockResolvedValue(pageOf([partner]));
  apiMocks.fetchAccounts.mockResolvedValue([]);

  render(<PartnerHarness />);

  await waitFor(() => expect(screen.getByText('Cliente Test')).toBeInTheDocument());
  await user.click(screen.getByRole('button', { name: 'Modifica' }));
  expect(screen.getByRole('heading', { name: 'Modifica soggetto' })).toBeInTheDocument();
  expect(screen.getByLabelText('Codice')).toHaveValue('CLI-001');
});

function PartnerHarness() {
  const flow = usePartnerFlow({ enabled: true, canLinkAccounts: true, executeCommand });
  return <PartnerExperience flow={flow} canManage canLinkAccounts busy={false} />;
}

function pageOf<T>(content: T[]): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 8,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true
  };
}
