import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { money } from '../utils/formatters';
import DashboardPage from './DashboardPage';

describe('DashboardPage', () => {
  it('mostra i KPI aggregati e gli stati vuoti', () => {
    render(
      <DashboardPage
        stats={{ products: 12, inventoryValue: 1234.5, lowStock: 2, outOfStock: 1, orders: 4, revenue: 820 }}
        recentOrders={[]}
        recentMovements={[]}
      />
    );

    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText((_, element) => element?.tagName === 'STRONG' && element.textContent === money.format(1234.5))).toBeInTheDocument();
    expect(screen.getByText('Scorte basse')).toBeInTheDocument();
    expect(screen.getByText('Ultimi ordini')).toBeInTheDocument();
    expect(screen.getByText('Movimenti recenti')).toBeInTheDocument();
    expect(screen.getAllByText('Nessun dato disponibile.')).toHaveLength(2);
  });
});
