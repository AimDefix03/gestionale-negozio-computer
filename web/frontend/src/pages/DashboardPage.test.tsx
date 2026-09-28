import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { money } from '../utils/formatters';
import DashboardPage from './DashboardPage';

describe('DashboardPage', () => {
  it('mostra i KPI aggregati e gli stati vuoti', () => {
    render(
      <DashboardPage
        stats={{ products: 12, potentialRetailStockValue: 1234.5, knownInventoryCostValue: 600, potentialGrossMarginOnCostedStock: 400, costedUnits: 8, uncostedUnits: 2, costCoveragePercentage: 80, lowStock: 2, outOfStock: 1, orders: { totalOrders: 4, draftOrders: 1, confirmedOrders: 1, fulfilledOrders: 1, canceledOrders: 1, draftOrderValue: 100, confirmedOrderValue: 200, fulfilledOrderValue: 300, grossCollected: 280, refunded: 30, netCollected: 250 } }}
        recentOrders={[]}
        recentMovements={[]}
      />
    );

    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText((_, element) => element?.tagName === 'STRONG' && element.textContent === money.format(1234.5))).toBeInTheDocument();
    expect(screen.getByText('Valore potenziale')).toBeInTheDocument();
    expect(screen.getByText('Valore noto a costo')).toBeInTheDocument();
    expect(screen.getByText('Copertura costo')).toBeInTheDocument();
    expect(screen.getByText('Margine potenziale')).toBeInTheDocument();
    expect(screen.getByText('2 unita senza costo documentato')).toBeInTheDocument();
    expect(screen.getByText('Ordini in bozza')).toBeInTheDocument();
    expect(screen.getByText('Ordini confermati')).toBeInTheDocument();
    expect(screen.getByText('Ordini evasi')).toBeInTheDocument();
    expect(screen.getByText('Incassato lordo')).toBeInTheDocument();
    expect(screen.getByText('Incassato netto')).toBeInTheDocument();
    expect(screen.getByText(/30,00.*rimborsato o stornato/)).toBeInTheDocument();
    expect(screen.getByText('4 ordini totali · 1 annullati')).toBeInTheDocument();
    expect(screen.getByText('Scorte basse')).toBeInTheDocument();
    expect(screen.getByText('Ultimi ordini')).toBeInTheDocument();
    expect(screen.getByText('Movimenti recenti')).toBeInTheDocument();
    expect(screen.getAllByText('Nessun dato disponibile.')).toHaveLength(2);
  });
});
