import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { CompanySettings } from '../api';
import CompanySettingsPage from './CompanySettingsPage';
import { renderWithDrafts } from '../test/renderWithDrafts';

const settings: CompanySettings = {
  version: 3,
  configured: true,
  missingDocumentFields: [],
  legalName: 'Azienda Test',
  taxCode: 'CFTEST',
  vatNumber: 'IT001',
  email: 'azienda@example.com',
  phone: '',
  address: 'Via Test 1',
  postalCode: '80100',
  city: 'Napoli',
  province: 'NA',
  countryCode: 'IT',
  timeZone: 'Europe/Rome',
  defaultVatRate: 0.22,
  invoicePrefix: 'FS',
  creditNotePrefix: 'NC',
  numberPadding: 4,
  updatedAt: '2026-07-14T12:00:00+02:00',
  updatedBy: 'admin'
};

describe('CompanySettingsPage', () => {
  it('converte la percentuale IVA nel valore decimale dell API', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    renderWithDrafts(<CompanySettingsPage settings={settings} busy={false} onSave={onSave} onReload={vi.fn()} />);

    const vat = screen.getByLabelText('Aliquota IVA predefinita %');
    await user.clear(vat);
    await user.type(vat, '10');
    await user.click(screen.getByRole('button', { name: 'Salva configurazione' }));

    expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ version: 3, defaultVatRate: 0.1, timeZone: 'Europe/Rome' }));
  });

  it('mostra anteprima e stato della configurazione', () => {
    renderWithDrafts(<CompanySettingsPage settings={settings} busy={false} onSave={vi.fn()} onReload={vi.fn()} />);

    expect(screen.getByText('Configurata')).toBeInTheDocument();
    expect(screen.getByText(`FS-${new Date().getFullYear()}-0001`)).toBeInTheDocument();
    expect(screen.getByText(/non costituiscono fatturazione elettronica/i)).toBeInTheDocument();
  });

  it('rende visibili i dati obbligatori mancanti e il fuso orario', () => {
    renderWithDrafts(<CompanySettingsPage settings={{ ...settings, configured: false, legalName: '', address: '', missingDocumentFields: ['legalName', 'address', 'timeZone'] }} busy={false} onSave={vi.fn()} onReload={vi.fn()} />);

    expect(screen.getByText('Da completare')).toBeInTheDocument();
    expect(screen.getByText(/ragione sociale, indirizzo, fuso orario/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Fuso orario aziendale')).toHaveValue('Europe/Rome');
  });
});
