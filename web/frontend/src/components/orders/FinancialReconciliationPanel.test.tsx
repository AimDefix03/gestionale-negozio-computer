import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import FinancialReconciliationPanel from './FinancialReconciliationPanel';

describe('FinancialReconciliationPanel', () => {
  it('mostra in modo esplicito un drift senza suggerire una correzione automatica', () => {
    render(
      <FinancialReconciliationPanel
        reconciliation={{
          generatedAt: '2026-08-05T10:00:00',
          balanced: false,
          checkedPayments: 4,
          checkedReturns: 1,
          mismatchCount: 1,
          counts: { PAYMENT_PAID_LEDGER_DRIFT: 1 },
          mismatches: [{
            type: 'PAYMENT_PAID_LEDGER_DRIFT',
            aggregateType: 'PAYMENT',
            aggregateId: 12,
            aggregateCode: 'ORD-0012',
            orderCode: 'ORD-0012',
            materializedAmount: 90,
            ledgerAmount: 100,
            detail: 'Il totale incassato materializzato diverge dal ledger.'
          }]
        }}
      />
    );

    expect(screen.getByText('1 anomalie')).toBeInTheDocument();
    expect(screen.getByText('Il totale incassato materializzato diverge dal ledger.')).toBeInTheDocument();
    expect(screen.getByText(/Proiezione/)).toHaveTextContent('Ledger');
    expect(screen.queryByRole('button', { name: /correggi/i })).not.toBeInTheDocument();
  });
});
