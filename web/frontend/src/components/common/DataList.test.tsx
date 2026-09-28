import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import DataList from './DataList';

describe('DataList', () => {
  it('espone caption, intestazioni di colonna e intestazione di riga', () => {
    render(<DataList title="Documenti fiscali" columns={['Codice', 'Tipo']} rows={[[<span key="code">DOC-001</span>, 'Fattura']]} />);

    const table = screen.getByRole('table', { name: 'Documenti fiscali' });
    expect(within(table).getByRole('columnheader', { name: 'Codice' })).toHaveAttribute('scope', 'col');
    expect(within(table).getByRole('columnheader', { name: 'Tipo' })).toHaveAttribute('scope', 'col');
    expect(within(table).getByRole('rowheader', { name: 'DOC-001' })).toHaveAttribute('scope', 'row');
  });

  it('mantiene una riga vuota coerente con il numero di colonne', () => {
    render(<DataList title="Account" columns={['Username', 'Ruolo']} rows={[]} actions={() => null} />);
    expect(screen.getByText('Nessun dato disponibile.')).toHaveAttribute('colspan', '3');
  });
});
