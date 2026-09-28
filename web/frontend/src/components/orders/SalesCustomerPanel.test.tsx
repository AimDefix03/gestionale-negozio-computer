import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { PartnerQuery } from '../../api';
import SalesCustomerPanel from './SalesCustomerPanel';

const customer = {
  id: 42,
  code: 'CLI-0042',
  type: 'CUSTOMER' as const,
  typeLabel: 'Cliente',
  displayName: 'Cliente Censito',
  taxCode: '',
  vatNumber: '',
  email: 'cliente@example.invalid',
  phone: '',
  address: '',
  city: 'Napoli',
  notes: '',
  active: true,
  createdAt: '2026-08-16T10:00:00',
  updatedAt: '2026-08-16T10:00:00'
};

const page = { content: [customer], page: 0, size: 6, totalElements: 1, totalPages: 1, first: true, last: true };

describe('SalesCustomerPanel', () => {
  it('seleziona un cliente censito tramite la sua identita stabile', async () => {
    const onPartnerSelect = vi.fn();
    const onQueryChange = vi.fn();
    function TestPanel() {
      const [query, setQuery] = useState<PartnerQuery>({ page: 0, size: 6, type: 'CUSTOMER', active: true });
      return (
        <SalesCustomerPanel
          mode="REGISTERED"
          page={page}
          query={query}
          walkInCustomerName=""
          busy={false}
          onModeChange={vi.fn()}
          onQueryChange={(nextQuery) => {
            setQuery(nextQuery);
            onQueryChange(nextQuery);
          }}
          onPartnerSelect={onPartnerSelect}
          onWalkInCustomerNameChange={vi.fn()}
        />
      );
    }
    render(<TestPanel />);

    await userEvent.type(screen.getByRole('textbox', { name: 'Cerca cliente' }), 'Censito');
    await userEvent.click(screen.getByRole('button', { name: /Cliente Censito.*CLI-0042/i }));

    expect(onQueryChange).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'Censito', page: 0 }));
    expect(onPartnerSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 42, code: 'CLI-0042' }));
  });

  it('non espone la scelta di altri clienti nel self service', () => {
    render(
      <SalesCustomerPanel
        selfServiceUsername="cliente_self"
        mode="REGISTERED"
        page={page}
        query={{}}
        walkInCustomerName=""
        busy={false}
        onModeChange={vi.fn()}
        onQueryChange={vi.fn()}
        onPartnerSelect={vi.fn()}
        onWalkInCustomerNameChange={vi.fn()}
      />
    );

    expect(screen.getByText('cliente_self')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cliente occasionale' })).not.toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'Cerca cliente' })).not.toBeInTheDocument();
  });
});
