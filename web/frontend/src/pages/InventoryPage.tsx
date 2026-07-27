import { FormEvent } from 'react';
import { MovementQuery, PageResponse, ProductLookup, StockMovement } from '../api';
import PaginationControls from '../components/PaginationControls';
import DataList from '../components/common/DataList';
import { MovementFormState } from '../types/ui';
import { dateTime } from '../utils/formatters';

type Props = {
  page: PageResponse<StockMovement>;
  query: MovementQuery;
  form: MovementFormState;
  products: ProductLookup[];
  busy: boolean;
  pageSize: number;
  onQueryChange: (query: MovementQuery) => void;
  onFormChange: (form: MovementFormState) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function InventoryPage({ page, query, form, products, busy, pageSize, onQueryChange, onFormChange, onSubmit }: Props) {
  return (
    <div className="content-grid two">
      <section className="panel">
        <div className="section-heading compact"><span>Operazione</span><h2>Movimento magazzino</h2><p>Registra carichi e scarichi con causale.</p></div>
        <form className="form-grid single" onSubmit={onSubmit}>
          <label>Prodotto<select value={form.productCode} onChange={(event) => onFormChange({ ...form, productCode: event.target.value })} required><option value="">Seleziona</option>{products.map((product) => <option key={product.code} value={product.code}>{product.code} - {product.name}</option>)}</select></label>
          <label>Tipo<select value={form.type} onChange={(event) => onFormChange({ ...form, type: event.target.value as MovementFormState['type'] })}><option value="LOAD">Carico</option><option value="UNLOAD">Scarico</option></select></label>
          <label>Quantita<input type="number" min="1" value={form.quantity} onChange={(event) => onFormChange({ ...form, quantity: event.target.value })} /></label>
          <label>Causale<input value={form.reason} onChange={(event) => onFormChange({ ...form, reason: event.target.value })} required /></label>
          <button className="button primary" disabled={busy}>Registra movimento</button>
        </form>
      </section>
      <DataList
        title="Movimenti magazzino"
        rows={page.content.map((movement) => [movement.typeLabel, movement.productCode, movement.reason, dateTime.format(new Date(movement.timestamp))])}
        footer={<PaginationControls page={page} onPageChange={(nextPage) => onQueryChange({ ...query, page: nextPage })} />}
      >
        <div className="list-filters">
          <label>Cerca<input value={query.q ?? ''} placeholder="Prodotto, causale o operatore" onChange={(event) => onQueryChange({ ...query, q: event.target.value, page: 0 })} /></label>
          <label>Tipo<select value={query.type ?? 'ALL'} onChange={(event) => onQueryChange({ ...query, type: event.target.value as MovementQuery['type'], page: 0 })}><option value="ALL">Tutti</option><option value="LOAD">Carico</option><option value="UNLOAD">Scarico</option><option value="RETURN">Reso cliente</option></select></label>
          <label>Prodotto<select value={query.productCode ?? ''} onChange={(event) => onQueryChange({ ...query, productCode: event.target.value, page: 0 })}><option value="">Tutti</option>{products.map((product) => <option key={product.code} value={product.code}>{product.code}</option>)}</select></label>
          <button className="button secondary compact-button" type="button" onClick={() => onQueryChange({ page: 0, size: pageSize })}>Reset</button>
        </div>
      </DataList>
    </div>
  );
}
