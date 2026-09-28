import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import CartPanel from './CartPanel';

const product = {
  id: 1,
  code: 'GPU-001',
  name: 'Scheda grafica',
  description: 'Prodotto per test vendita.',
  category: 'HARDWARE' as const,
  brand: 'TestBrand',
  productType: 'Scheda grafica',
  usageContext: '',
  quantity: 3,
  reservedQuantity: 0,
  availableQuantity: 3,
  price: 100,
  discount: 0,
  discountedPrice: 100,
  discontinued: false
};

describe('CartPanel', () => {
  it('espone metodi strutturati e comunica la selezione', async () => {
    const onPaymentMethodChange = vi.fn();
    render(
      <CartPanel
        cart={[]}
        customerLabel="Cliente da selezionare"
        customerReady={false}
        paymentMethod="CARD"
        busy={false}
        onPaymentMethodChange={onPaymentMethodChange}
        onCheckout={vi.fn()}
        onClear={vi.fn()}
        onQuantityChange={vi.fn()}
      />
    );

    const selector = screen.getByRole('combobox', { name: /metodo di pagamento/i });
    expect(selector).toHaveValue('CARD');
    expect(screen.getByRole('option', { name: 'Bonifico bancario' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crea bozza ordine' })).toBeDisabled();

    await userEvent.selectOptions(selector, 'CASH');

    expect(onPaymentMethodChange).toHaveBeenCalledWith('CASH');
  });

  it('mostra prezzi e permette di correggere la quantita prima dell invio', async () => {
    const onQuantityChange = vi.fn();
    render(
      <CartPanel
        cart={[{ product, quantity: 2, maximumQuantity: 3 }]}
        customerLabel="Cliente Censito"
        customerReady
        paymentMethod="CARD"
        busy={false}
        onPaymentMethodChange={vi.fn()}
        onCheckout={vi.fn()}
        onClear={vi.fn()}
        onQuantityChange={onQuantityChange}
      />
    );

    expect(screen.getByText('Cliente Censito')).toBeInTheDocument();
    expect(screen.getAllByText(/200,00/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: 'Crea bozza ordine' })).toBeEnabled();

    await userEvent.click(screen.getByRole('button', { name: 'Riduci quantita Scheda grafica' }));

    expect(onQuantityChange).toHaveBeenCalledWith('GPU-001', 1);
  });

  it('non ricostruisce un limite numerico per i prodotti della projection cliente', () => {
    const customerProduct = {
      code: 'GPU-CUSTOMER',
      name: 'Prodotto cliente',
      description: 'Projection commerciale.',
      category: 'HARDWARE' as const,
      brand: 'Example Brand',
      productType: 'Scheda grafica',
      usageContext: '',
      price: 100,
      discount: 0,
      discountedPrice: 100
    };
    render(
      <CartPanel
        cart={[{ product: customerProduct, quantity: 8 }]}
        customerLabel="cliente"
        customerReady
        paymentMethod="CARD"
        busy={false}
        onPaymentMethodChange={vi.fn()}
        onCheckout={vi.fn()}
        onClear={vi.fn()}
        onQuantityChange={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: 'Aumenta quantita Prodotto cliente' })).toBeEnabled();
    expect(screen.queryByText(/disponibil/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/giacenza/i)).not.toBeInTheDocument();
  });
});
