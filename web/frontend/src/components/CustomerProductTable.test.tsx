import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import CustomerProductTable from './CustomerProductTable';

const limitedProduct = {
  code: 'GPU-CUSTOMER',
  name: 'Scheda grafica',
  description: 'Prodotto visibile nel catalogo cliente.',
  category: 'HARDWARE' as const,
  brand: 'Example Brand',
  productType: 'Scheda grafica',
  usageContext: 'Professionale',
  price: 100,
  discount: 10,
  discountedPrice: 90,
  availability: 'LIMITED' as const,
  availabilityLabel: 'Disponibilita limitata'
};

describe('CustomerProductTable', () => {
  it('mostra solo la projection commerciale e non offre ordinamenti per quantita', async () => {
    const onAddToCart = vi.fn();
    render(
      <CustomerProductTable
        page={{ content: [limitedProduct], page: 0, size: 8, totalElements: 1, totalPages: 1, first: true, last: true }}
        query={{ page: 0, size: 8, sort: 'NAME_ASC' }}
        context="sales"
        busy={false}
        onQueryChange={vi.fn()}
        onPageChange={vi.fn()}
        onAddToCart={onAddToCart}
      />
    );

    expect(screen.getAllByText('Disponibilita limitata')).toHaveLength(2);
    expect(screen.queryByText(/giacenza/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/riservat/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /quantita/i })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Aggiungi' }));
    expect(onAddToCart).toHaveBeenCalledWith(limitedProduct);
  });

  it('impedisce di aggiungere un prodotto commercialmente non disponibile', () => {
    render(
      <CustomerProductTable
        page={{ content: [{ ...limitedProduct, availability: 'UNAVAILABLE', availabilityLabel: 'Non disponibile' }], page: 0, size: 8, totalElements: 1, totalPages: 1, first: true, last: true }}
        query={{ page: 0, size: 8 }}
        context="sales"
        busy={false}
        onQueryChange={vi.fn()}
        onPageChange={vi.fn()}
        onAddToCart={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: 'Aggiungi' })).toBeDisabled();
  });
});
