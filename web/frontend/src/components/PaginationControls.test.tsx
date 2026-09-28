import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { PageResponse } from '../api';
import PaginationControls from './PaginationControls';

function page(overrides: Partial<PageResponse<unknown>> = {}): PageResponse<unknown> {
  return {
    content: [],
    page: 0,
    size: 10,
    totalElements: 24,
    totalPages: 3,
    first: true,
    last: false,
    ...overrides
  };
}

describe('PaginationControls', () => {
  it('disabilita la navigazione indietro sulla prima pagina', () => {
    render(<PaginationControls page={page()} onPageChange={vi.fn()} />);

    expect(screen.getByText('1-10 di 24')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Prima' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Indietro' })).toBeDisabled();
    expect(screen.getByText('Pagina 1 / 3')).toBeInTheDocument();
  });

  it('inoltra la pagina richiesta dai controlli disponibili', async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();
    render(<PaginationControls page={page({ page: 1, first: false })} onPageChange={onPageChange} />);

    expect(screen.getByText('11-20 di 24')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Prima' }));
    await user.click(screen.getByRole('button', { name: 'Indietro' }));
    await user.click(screen.getByRole('button', { name: 'Avanti' }));

    expect(onPageChange.mock.calls).toEqual([[0], [0], [2]]);
  });

  it('gestisce una pagina vuota senza intervalli negativi', () => {
    render(<PaginationControls page={page({ totalElements: 0, totalPages: 0, last: true })} onPageChange={vi.fn()} />);

    expect(screen.getByText('0-0 di 0')).toBeInTheDocument();
    expect(screen.getByText('Pagina 0 / 0')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Avanti' })).toBeDisabled();
  });
});
