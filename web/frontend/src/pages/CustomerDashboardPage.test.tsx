import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import CustomerDashboardPage from './CustomerDashboardPage';

describe('CustomerDashboardPage', () => {
  it('mostra soltanto KPI e ordini personali', () => {
    render(
      <CustomerDashboardPage dashboard={{
        totalOrders: 4,
        draftOrders: 1,
        confirmedOrders: 1,
        fulfilledOrders: 2,
        canceledOrders: 0,
        recentOrders: [{
          code: 'ORD-1001',
          timestamp: '2026-08-17T10:00:00',
          total: 250,
          status: 'CONFIRMED',
          statusLabel: 'Confermato',
          paymentStatus: 'PENDING',
          paymentStatusLabel: 'Da incassare'
        }]
      }} />
    );

    expect(screen.getByText('I miei ordini')).toBeInTheDocument();
    expect(screen.getByText(/ORD-1001/)).toBeInTheDocument();
    expect(screen.queryByText(/inventario/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/movimenti/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/ricavi/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/scorte/i)).not.toBeInTheDocument();
  });
});
