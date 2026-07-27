import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ProductForm from './ProductForm';

describe('ProductForm', () => {
  it('converte e invia i dati di un nuovo prodotto', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ProductForm editingProduct={null} busy={false} onSubmit={onSubmit} onCancel={vi.fn()} />);

    await user.type(screen.getByLabelText('Codice'), 'GPU-001');
    await user.type(screen.getByLabelText('Nome'), 'Scheda video');
    await user.type(screen.getByLabelText('Brand'), 'Example Brand');
    await user.type(screen.getByLabelText('Tipo prodotto'), 'Scheda grafica');
    await user.type(screen.getByLabelText('Utilizzo opzionale'), 'Gaming');
    await user.clear(screen.getByLabelText('Quantita'));
    await user.type(screen.getByLabelText('Quantita'), '4');
    await user.clear(screen.getByLabelText('Prezzo'));
    await user.type(screen.getByLabelText('Prezzo'), '899.90');
    await user.clear(screen.getByLabelText('Sconto %'));
    await user.type(screen.getByLabelText('Sconto %'), '5');
    await user.type(screen.getByLabelText('Descrizione'), 'Prodotto di test per il catalogo.');
    await user.click(screen.getByRole('button', { name: 'Crea prodotto' }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledOnce());
    expect(onSubmit).toHaveBeenCalledWith({
      code: 'GPU-001',
      name: 'Scheda video',
      description: 'Prodotto di test per il catalogo.',
      category: 'HARDWARE',
      brand: 'Example Brand',
      productType: 'Scheda grafica',
      usageContext: 'Gaming',
      quantity: 4,
      price: 899.9,
      discount: 5
    });
  });

  it('mostra i dati esistenti e consente di annullare la modifica', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <ProductForm
        editingProduct={{
          id: 1,
          code: 'CPU-001',
          name: 'Processore',
          description: 'Descrizione',
          category: 'HARDWARE',
          brand: 'Example Brand',
          productType: 'CPU',
          usageContext: '',
          quantity: 2,
          reservedQuantity: 0,
          availableQuantity: 2,
          price: 399,
          discount: 0,
          discountedPrice: 399,
          discontinued: false
        }}
        busy={false}
        onSubmit={vi.fn().mockResolvedValue(undefined)}
        onCancel={onCancel}
      />
    );

    expect(screen.getByLabelText('Codice')).toHaveValue('CPU-001');
    expect(screen.getByRole('button', { name: 'Salva modifiche' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Annulla' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });
});
