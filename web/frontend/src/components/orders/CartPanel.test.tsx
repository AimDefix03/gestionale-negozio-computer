import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import CartPanel from './CartPanel';

describe('CartPanel', () => {
  it('espone metodi strutturati e comunica la selezione', async () => {
    const onPaymentMethodChange = vi.fn();
    render(
      <CartPanel
        cart={[]}
        paymentMethod="CARD"
        onPaymentMethodChange={onPaymentMethodChange}
        onCheckout={vi.fn()}
        onClear={vi.fn()}
      />
    );

    const selector = screen.getByRole('combobox', { name: /metodo di pagamento/i });
    expect(selector).toHaveValue('CARD');
    expect(screen.getByRole('option', { name: 'Bonifico bancario' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crea ordine' })).toBeDisabled();

    await userEvent.selectOptions(selector, 'CASH');

    expect(onPaymentMethodChange).toHaveBeenCalledWith('CASH');
  });
});
