import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import PhysicalInventoryPanel from './PhysicalInventoryPanel';

describe('PhysicalInventoryPanel', () => {
  it('richiede una motivazione e rende esplicita l’approvazione separata', async () => {
    const user = userEvent.setup();
    const approve = vi.fn();
    render(<PhysicalInventoryPanel flow={{
      query: { page: 0, size: 6 },
      page: { content: [], page: 0, size: 6, totalElements: 0, totalPages: 0, first: true, last: true },
      loading: false,
      refreshing: false,
      error: null,
      pageSize: 6,
      selectedId: 41,
      detail: {
        id: 41,
        version: 2,
        code: 'INV-0041',
        status: 'SUBMITTED',
        statusLabel: 'Da approvare',
        reason: 'Inventario periodico',
        createdAt: '2026-09-01T10:00:00Z',
        createdBy: 'operatore',
        createdByRole: 'Dipendente',
        submittedAt: '2026-09-01T10:15:00Z',
        submittedBy: 'operatore',
        submittedByRole: 'Dipendente',
        approvedAt: null,
        approvedBy: null,
        approvedByRole: null,
        approvalReason: null,
        canceledAt: null,
        canceledBy: null,
        cancellationReason: null,
        itemCount: 1,
        countedItems: 1,
        differenceItems: 1,
        totalAbsoluteDifference: 2,
        capabilities: { canCount: false, canSubmit: false, canApprove: true, canCancel: true },
        items: [{
          id: 52,
          productId: 7,
          productCode: 'GPU-001',
          productName: 'Scheda video',
          theoreticalQuantitySnapshot: 10,
          reservedQuantitySnapshot: 1,
          countedQuantity: 8,
          theoreticalQuantityAtCount: 10,
          reservedQuantityAtCount: 1,
          differenceQuantity: -2,
          countedAt: '2026-09-01T10:10:00Z',
          countedBy: 'operatore',
          countNote: 'Scaffale A',
          quantityBeforeApproval: null,
          quantityAfterApproval: null,
          reservedQuantityAtApproval: null,
          compensatedMovementDelta: null,
          stockMovementId: null
        }]
      },
      detailLoading: false,
      detailError: null,
      setQuery: vi.fn(),
      setSelectedId: vi.fn(),
      create: vi.fn(),
      count: vi.fn(),
      submit: vi.fn(),
      approve,
      cancel: vi.fn(),
      refreshResources: vi.fn(),
      reset: vi.fn()
    }} products={[]} busy={false} />);

    const approveButton = screen.getByRole('button', { name: 'Approva differenze' });
    expect(approveButton).toBeDisabled();
    expect(screen.getByText('Differenza -2')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Motivazione decisione'), 'Conteggio verificato');
    await user.click(approveButton);
    expect(approve).toHaveBeenCalledWith('Conteggio verificato');
  });
});
