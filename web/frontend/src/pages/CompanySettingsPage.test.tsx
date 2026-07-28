import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { CompanySettings } from '../api';
import CompanySettingsPage from './CompanySettingsPage';

const settings: CompanySettings = {
  version: 3,
  configured: true,
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
  defaultVatRate: 0.22,
  invoicePrefix: 'FS',
  creditNotePrefix: 'NC',
  numberPadding: 4,
  updatedAt: '2026-07-14T10:00:00',
  updatedBy: 'admin'
};

describe('CompanySettingsPage', () => {
  it('converte la percentuale IVA nel valore decimale dell API', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<CompanySettingsPage settings={settings} busy={false} onSave={onSave} onReload={vi.fn()} />);

    const vat = screen.getByLabelText('Aliquota IVA predefinita %');
    await user.clear(vat);
    await user.type(vat, '10');
    await user.click(screen.getByRole('button', { name: 'Salva configurazione' }));

    expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ version: 3, defaultVatRate: 0.1 }));
  });

  it('mostra anteprima e stato della configurazione', () => {
    render(<CompanySettingsPage settings={settings} busy={false} onSave={vi.fn()} onReload={vi.fn()} />);

    expect(screen.getByText('Configurata')).toBeInTheDocument();
    expect(screen.getByText(`FS-${new Date().getFullYear()}-0001`)).toBeInTheDocument();
    expect(screen.getByText(/non costituiscono fatturazione elettronica/i)).toBeInTheDocument();
  });
});
