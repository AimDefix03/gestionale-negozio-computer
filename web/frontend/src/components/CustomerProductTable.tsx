import { useMemo } from 'react';
import { CustomerProduct, PageResponse, ProductQuery } from '../api';
import PaginationControls from './PaginationControls';

type Props = {
  page: PageResponse<CustomerProduct>;
  query: ProductQuery;
  context?: 'catalog' | 'sales';
  busy: boolean;
  onQueryChange: (query: ProductQuery) => void;
  onPageChange: (page: number) => void;
  onAddToCart?: (product: CustomerProduct) => void;
};

type StockFilter = 'ALL' | 'AVAILABLE' | 'LOW' | 'OUT';
type CustomerSortMode = 'NAME_ASC' | 'PRICE_ASC' | 'PRICE_DESC';

const money = new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' });

export default function CustomerProductTable({ page, query, context = 'catalog', busy, onQueryChange, onPageChange, onAddToCart }: Props) {
  const brands = useMemo(() => uniqueValues(page.content.map((product) => product.brand)), [page.content]);
  const productTypes = useMemo(() => uniqueValues(page.content.map((product) => product.productType)), [page.content]);

  function updateQuery(nextQuery: ProductQuery) {
    onQueryChange({ ...query, ...nextQuery, page: 0 });
  }

  function resetFilters() {
    onQueryChange({ page: 0, size: query.size ?? 8, sort: 'NAME_ASC' });
  }

  return (
    <section className="panel catalog-panel customer-catalog-panel">
      <div className="panel-toolbar">
        <div className="section-heading compact">
          <span>{context === 'sales' ? 'Nuovo ordine' : 'Catalogo'}</span>
          <h2>{context === 'sales' ? 'Scegli i prodotti' : 'Prodotti disponibili'}</h2>
          <p>Prezzi e disponibilita commerciale, senza informazioni interne di magazzino.</p>
        </div>
        <span className="result-pill">{page.content.length} / {page.totalElements}</span>
      </div>

      <div className="catalog-controls customer-catalog-controls">
        <label className="search-field">
          Cerca prodotto
          <input value={query.q ?? ''} placeholder="Codice, nome, brand o tipo" onChange={(event) => updateQuery({ q: event.target.value })} />
        </label>
        <label>
          Categoria
          <select value={query.category ?? 'ALL'} onChange={(event) => updateQuery({ category: event.target.value as ProductQuery['category'] })}>
            <option value="ALL">Tutte</option>
            <option value="HARDWARE">Hardware</option>
            <option value="SOFTWARE">Software</option>
          </select>
        </label>
        <label>
          Brand
          <select value={query.brand ?? 'ALL'} onChange={(event) => updateQuery({ brand: event.target.value })}>
            <option value="ALL">Tutti</option>
            {brands.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>
        <label>
          Tipo
          <select value={query.productType ?? 'ALL'} onChange={(event) => updateQuery({ productType: event.target.value })}>
            <option value="ALL">Tutti</option>
            {productTypes.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>
        <label>
          Disponibilita
          <select value={query.stock ?? 'ALL'} onChange={(event) => updateQuery({ stock: event.target.value as StockFilter })}>
            <option value="ALL">Tutte</option>
            <option value="AVAILABLE">Disponibile</option>
            <option value="LOW">Disponibilita limitata</option>
            <option value="OUT">Non disponibile</option>
          </select>
        </label>
        <label>
          Ordina
          <select value={query.sort ?? 'NAME_ASC'} onChange={(event) => updateQuery({ sort: event.target.value as CustomerSortMode })}>
            <option value="NAME_ASC">Nome A-Z</option>
            <option value="PRICE_ASC">Prezzo crescente</option>
            <option value="PRICE_DESC">Prezzo decrescente</option>
          </select>
        </label>
        <button className="button secondary compact-button" type="button" onClick={resetFilters}>Reset</button>
      </div>

      {page.content.length === 0 ? (
        <div className="empty-state">Nessun prodotto corrisponde ai filtri selezionati.</div>
      ) : (
        <div className="table-shell catalog-table-shell">
          <table>
            <caption className="sr-only">{context === 'sales' ? 'Prodotti disponibili per il nuovo ordine' : 'Catalogo prodotti disponibili'}</caption>
            <thead><tr><th scope="col">Codice</th><th scope="col">Prodotto</th><th scope="col">Classificazione</th><th scope="col">Disponibilita</th><th scope="col">Prezzo</th>{onAddToCart && <th scope="col">Azioni</th>}</tr></thead>
            <tbody>{page.content.map((product) => (
              <tr key={product.code}>
                <th scope="row" className="strong">{product.code}</th>
                <td><div className="product-cell"><strong>{product.name}</strong><span>{product.description}</span></div></td>
                <td><div className="classification-stack"><strong>{product.brand}</strong><span>{product.productType}</span><small>{product.category}{product.usageContext ? ` · ${product.usageContext}` : ''}</small></div></td>
                <td><span className={`status-badge ${availabilityClass(product.availability)}`}>{product.availabilityLabel}</span></td>
                <td><div className="price-stack"><strong>{money.format(product.discountedPrice)}</strong>{product.discount > 0 && <span>{product.discount}% sconto</span>}</div></td>
                {onAddToCart && <td><button className="link-button" disabled={busy || product.availability === 'UNAVAILABLE'} onClick={() => onAddToCart(product)}>Aggiungi</button></td>}
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
      <PaginationControls page={page} onPageChange={onPageChange} />
    </section>
  );
}

function uniqueValues(values: string[]) {
  return Array.from(new Set(values.filter(Boolean))).sort((first, second) => first.localeCompare(second));
}

function availabilityClass(availability: CustomerProduct['availability']) {
  if (availability === 'AVAILABLE') return 'ok';
  if (availability === 'LIMITED') return 'low';
  return 'out';
}
