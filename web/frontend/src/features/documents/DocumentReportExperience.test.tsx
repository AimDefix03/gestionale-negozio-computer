import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, it, vi } from 'vitest';
import type { FiscalDocument, PageResponse } from '../../api';
import type { CommandExecutionOptions, CommandExecutor } from '../../hooks/useCommandExecution';
import DocumentReportExperience from './DocumentReportExperience';
import useDocumentReportFlow from './useDocumentReportFlow';

const apiMocks = vi.hoisted(() => ({
  createCreditNote: vi.fn(),
  createInvoice: vi.fn(),
  downloadInventoryReport: vi.fn(),
  downloadSalesReport: vi.fn(),
  fetchDocumentPage: vi.fn(),
  fetchInventoryReport: vi.fn(),
  fetchSalesReport: vi.fn()
}));

vi.mock('../../api', async () => ({
  ...await vi.importActual<typeof import('../../api')>('../../api'),
  ...apiMocks
}));

const document: FiscalDocument = {
  id: 1,
  code: 'FS-2026-0001',
  fiscalYear: 2026,
  sequenceNumber: 1,
  documentPrefix: 'FS',
  type: 'SIMULATED_INVOICE',
  typeLabel: 'Fattura simulata',
  status: 'ISSUED',
  statusLabel: 'Emessa',
  createdAt: '2026-08-18T10:00:00Z',
  relatedOrderCode: 'ORD-0001',
  customer: 'Cliente Test',
  companySnapshotLegalName: 'Negozio Test',
  companySnapshotTaxCode: '',
  companySnapshotVatNumber: '',
  companySnapshotEmail: '',
  companySnapshotPhone: '',
  companySnapshotAddress: '',
  companySnapshotPostalCode: '',
  companySnapshotCity: 'Napoli',
  companySnapshotProvince: 'NA',
  companySnapshotCountryCode: 'IT',
  companySnapshotTimeZone: 'Europe/Rome',
  customerSnapshotCode: 'CLI-001',
  customerSnapshotName: 'Cliente Test',
  customerSnapshotTaxCode: '',
  customerSnapshotVatNumber: '',
  customerSnapshotEmail: '',
  customerSnapshotPhone: '',
  customerSnapshotAddress: '',
  customerSnapshotCity: 'Napoli',
  paymentMethod: 'CARD',
  taxableAmount: 100,
  vatRate: 0.22,
  vatAmount: 22,
  totalAmount: 122,
  createdBy: 'admin',
  createdByRole: 'ADMIN',
  reason: '',
  disclaimer: 'Documento simulato',
  capabilities: { canCreateCreditNote: true }
};

const executeCommand: CommandExecutor = async <T,>(options: CommandExecutionOptions<T>) => {
  const value = await options.command();
  options.applyResponse(value);
  options.afterConfirmed?.(value);
  return { status: 'saved', value };
};

it('compone documenti e nota credito senza stato del dominio in App', async () => {
  const user = userEvent.setup();
  const creditNote = { ...document, id: 2, code: 'NC-2026-0001', type: 'SIMULATED_CREDIT_NOTE' as const, typeLabel: 'Nota di credito simulata' };
  apiMocks.fetchDocumentPage.mockResolvedValue(pageOf([document]));
  apiMocks.createCreditNote.mockResolvedValue(creditNote);

  render(<DocumentHarness />);

  await waitFor(() => expect(screen.getByText('FS-2026-0001')).toBeInTheDocument());
  await user.clear(screen.getByLabelText('Motivo nota credito'));
  await user.type(screen.getByLabelText('Motivo nota credito'), 'Reso concordato');
  await user.click(screen.getByRole('button', { name: 'Nota credito' }));
  await waitFor(() => expect(screen.getByText('NC-2026-0001')).toBeInTheDocument());
  expect(apiMocks.createCreditNote).toHaveBeenCalledWith('ORD-0001', 'Reso concordato');
});

it('mostra l errore iniziale documenti e rende disponibile il retry', async () => {
  const user = userEvent.setup();
  apiMocks.fetchDocumentPage.mockRejectedValue(new Error('Servizio documenti non disponibile'));

  render(<DocumentHarness />);

  expect(await screen.findByRole('heading', { name: 'Documenti non disponibili' })).toBeInTheDocument();
  expect(screen.getByText('Servizio documenti non disponibile')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Riprova' }));
  await waitFor(() => expect(apiMocks.fetchDocumentPage).toHaveBeenCalledTimes(2));
});

it('non propone la nota credito quando il server la nega anche se il filtro mostra solo la fattura', async () => {
  apiMocks.fetchDocumentPage.mockResolvedValue(pageOf([{
    ...document,
    capabilities: { canCreateCreditNote: false }
  }]));

  render(<DocumentHarness />);

  await waitFor(() => expect(screen.getByText('FS-2026-0001')).toBeInTheDocument());
  expect(screen.queryByRole('button', { name: 'Nota credito' })).not.toBeInTheDocument();
  expect(screen.getByText('Nessuna azione')).toBeInTheDocument();
});

function DocumentHarness() {
  const flow = useDocumentReportFlow({
    documentsEnabled: true,
    reportsEnabled: false,
    reportsActive: false,
    executeCommand,
    onOpenDocuments: vi.fn(),
    onNotice: vi.fn(),
    onError: vi.fn()
  });
  return <DocumentReportExperience view="documents" flow={flow} busy={false} />;
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
