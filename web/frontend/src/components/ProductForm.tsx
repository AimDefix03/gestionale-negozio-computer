import { FormEvent, useEffect, useState } from 'react';
import { Product, ProductCategory, ProductPayload } from '../api';

type Props = {
  editingProduct: Product | null;
  busy: boolean;
  onSubmit: (payload: ProductPayload) => Promise<void>;
  onCancel: () => void;
};

const initialForm = {
  code: '',
  name: '',
  description: '',
  category: 'HARDWARE' as ProductCategory,
  brand: '',
  productType: '',
  usageContext: '',
  quantity: '0',
  price: '0',
  discount: '0'
};

export default function ProductForm({ editingProduct, busy, onSubmit, onCancel }: Props) {
  const [form, setForm] = useState(initialForm);

  useEffect(() => {
    if (!editingProduct) {
      setForm(initialForm);
      return;
    }

    setForm({
      code: editingProduct.code,
      name: editingProduct.name,
      description: editingProduct.description,
      category: editingProduct.category,
      brand: editingProduct.brand,
      productType: editingProduct.productType,
      usageContext: editingProduct.usageContext,
      quantity: String(editingProduct.quantity),
      price: String(editingProduct.price),
      discount: String(editingProduct.discount)
    });
  }, [editingProduct]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSubmit({
      code: form.code,
      name: form.name,
      description: form.description,
      category: form.category,
      brand: form.brand,
      productType: form.productType,
      usageContext: form.usageContext,
      quantity: Number(form.quantity),
      price: Number(form.price),
      discount: Number(form.discount)
    });
  }

  function updateField<Key extends keyof typeof form>(key: Key, value: (typeof form)[Key]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  return (
    <form className="product-form" onSubmit={handleSubmit}>
      <div className="section-heading compact">
        <span>Catalogo</span>
        <h2>{editingProduct ? 'Modifica prodotto' : 'Nuovo prodotto'}</h2>
        <p>Gestisci anagrafica, classificazione e disponibilita.</p>
      </div>

      <div className="form-grid">
        <label>
          Codice
          <input value={form.code} onChange={(event) => updateField('code', event.target.value)} required />
        </label>
        <label>
          Nome
          <input value={form.name} onChange={(event) => updateField('name', event.target.value)} required />
        </label>
        <label>
          Categoria
          <select value={form.category} onChange={(event) => updateField('category', event.target.value as ProductCategory)}>
            <option value="HARDWARE">Hardware</option>
            <option value="SOFTWARE">Software</option>
          </select>
        </label>
        <label>
          Brand
          <input value={form.brand} onChange={(event) => updateField('brand', event.target.value)} required />
        </label>
        <label>
          Tipo prodotto
          <input value={form.productType} onChange={(event) => updateField('productType', event.target.value)} required />
        </label>
        <label>
          Utilizzo opzionale
          <input value={form.usageContext} onChange={(event) => updateField('usageContext', event.target.value)} />
        </label>
        <label>
          Quantita
          <input type="number" min="0" value={form.quantity} onChange={(event) => updateField('quantity', event.target.value)} required />
        </label>
        <label>
          Prezzo
          <input type="number" min="0" step="0.01" value={form.price} onChange={(event) => updateField('price', event.target.value)} required />
        </label>
        <label>
          Sconto %
          <input type="number" min="0" max="100" step="0.01" value={form.discount} onChange={(event) => updateField('discount', event.target.value)} required />
        </label>
        <label className="wide">
          Descrizione
          <textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} required />
        </label>
      </div>

      <div className="form-actions">
        {editingProduct && <button className="button secondary" type="button" onClick={onCancel}>Annulla</button>}
        <button className="button primary" type="submit" disabled={busy}>{busy ? 'Salvataggio...' : editingProduct ? 'Salva modifiche' : 'Crea prodotto'}</button>
      </div>
    </form>
  );
}
