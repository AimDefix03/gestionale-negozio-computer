import { useMemo } from 'react';
import { PageResponse, Product, ProductLookup, ProductQuery } from '../api';
import PaginationControls from './PaginationControls';

type Props = {
  page: PageResponse<Product>;
  filterSource: ProductLookup[];
  query: ProductQuery;
  selectedCodes: string[];
  activeProductCode?: string;
  canManage: boolean;
  onQueryChange: (query: ProductQuery) => void;
  onPageChange: (page: number) => void;
  onSelectionChange: (codes: string[]) => void;
  onView: (product: Product) => void;
  onEdit: (product: Product) => void;
  onDeleteOne: (code: string) => void;
  onDiscontinue: (code: string) => void;
  onDeleteSelected: () => void;
  onAddToCart?: (product: Product) => void;
};

type StockFilter = 'ALL' | 'AVAILABLE' | 'LOW' | 'OUT';
type SortMode = 'NAME_ASC' | 'PRICE_ASC' | 'PRICE_DESC' | 'QTY_ASC' | 'QTY_DESC';

const money = new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' });

export default function ProductTable({ page, filterSource, query, selectedCodes, activeProductCode, canManage, onQueryChange, onPageChange, onSelectionChange, onView, onEdit, onDeleteOne, onDiscontinue, onDeleteSelected, onAddToCart }: Props) {
  const products = page.content;
  const brands = useMemo(() => uniqueValues(filterSource.map((product) => product.brand)), [filterSource]);
  const productTypes = useMemo(() => uniqueValues(filterSource.map((product) => product.productType)), [filterSource]);
  const visibleCodes = products.map((product) => product.code);
  const allVisibleSelected = visibleCodes.length > 0 && visibleCodes.every((code) => selectedCodes.includes(code));

  function updateQuery(nextQuery: ProductQuery) {
    onQueryChange({ ...query, ...nextQuery, page: 0 });
  }

  function toggleAll() {
    if (allVisibleSelected) {
      onSelectionChange(selectedCodes.filter((code) => !visibleCodes.includes(code)));
      return;
    }

    onSelectionChange(Array.from(new Set([...selectedCodes, ...visibleCodes])));
  }

  function toggleProduct(code: string) {
    onSelectionChange(selectedCodes.includes(code)
      ? selectedCodes.filter((selectedCode) => selectedCode !== code)
      : [...selectedCodes, code]
    );
  }

  function resetFilters() {
    onQueryChange({ page: 0, size: query.size ?? 8, sort: 'NAME_ASC' });
  }

  return (
    <section className="panel catalog-panel">
      <div className="panel-toolbar">
        <div className="section-heading compact">
          <span>Inventario</span>
          <h2>Catalogo prodotti</h2>
          <p>Ricerca, filtri e stato stock del catalogo operativo.</p>
        </div>
        <div className="toolbar-actions">
          <span className="result-pill">{page.content.length} / {page.totalElements}</span>
          {canManage && <button className="button danger" disabled={selectedCodes.length === 0} onClick={onDeleteSelected}>Elimina selezionati</button>}
        </div>
      </div>

      <div className="catalog-controls">
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
          Stock
          <select value={query.stock ?? 'ALL'} onChange={(event) => updateQuery({ stock: event.target.value as StockFilter })}>
            <option value="ALL">Tutto</option>
            <option value="AVAILABLE">Disponibile</option>
            <option value="LOW">Scorta bassa</option>
            <option value="OUT">Esaurito</option>
          </select>
        </label>
        <label>
          Ordina
          <select value={query.sort ?? 'NAME_ASC'} onChange={(event) => updateQuery({ sort: event.target.value as SortMode })}>
            <option value="NAME_ASC">Nome A-Z</option>
            <option value="PRICE_ASC">Prezzo crescente</option>
            <option value="PRICE_DESC">Prezzo decrescente</option>
            <option value="QTY_ASC">Quantita crescente</option>
            <option value="QTY_DESC">Quantita decrescente</option>
          </select>
        </label>
        <button className="button secondary compact-button" type="button" onClick={resetFilters}>Reset</button>
      </div>

      {filterSource.length === 0 ? (
        <div className="empty-state">Nessun prodotto presente. Crea il primo elemento del catalogo.</div>
      ) : products.length === 0 ? (
        <div className="empty-state">Nessun prodotto corrisponde ai filtri selezionati.</div>
      ) : (
        <div className="table-shell catalog-table-shell">
          <table>
            <thead>
              <tr>
                {canManage && <th><input type="checkbox" checked={allVisibleSelected} onChange={toggleAll} /></th>}
                <th>Codice</th>
                <th>Prodotto</th>
                <th>Classificazione</th>
                <th>Stock</th>
                <th>Prezzo</th>
                <th>Azioni</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => {
                const status = getStockStatus(product);

                return (
                  <tr key={product.id} className={activeProductCode === product.code ? 'active-row' : ''}>
                    {canManage && <td><input type="checkbox" checked={selectedCodes.includes(product.code)} onChange={() => toggleProduct(product.code)} /></td>}
                    <td className="strong">{product.code}</td>
                    <td>
                      <div className="product-cell">
                        <strong>{product.name}</strong>
                        <span>{product.description}</span>
                      </div>
                    </td>
                    <td>
                      <div className="classification-stack">
                        <strong>{product.brand}</strong>
                        <span>{product.productType}</span>
                        <small>{product.category}{product.usageContext ? ` · ${product.usageContext}` : ''}</small>
                      </div>
                    </td>
                    <td>
                      <div className="stock-stack">
                        <span className={`status-badge ${status.className}`}>{status.label}</span>
                        <strong>{product.availableQuantity} disponibili</strong>
                        <span>{product.quantity} giacenza · {product.reservedQuantity} riservate</span>
                      </div>
                    </td>
                    <td>
                      <div className="price-stack">
                        <strong>{money.format(product.discountedPrice)}</strong>
                        {product.discount > 0 && <span>{product.discount}% sconto</span>}
                      </div>
                    </td>
                    <td>
                      <div className="row-actions">
                        <button className="link-button" onClick={() => onView(product)}>Dettaglio</button>
                        {canManage ? (
                          <>
                            <button className="link-button" onClick={() => onEdit(product)}>Modifica</button>
                            {!product.discontinued && <button className="link-button" onClick={() => onDiscontinue(product.code)}>Disattiva</button>}
                            <button className="link-button danger-text" onClick={() => onDeleteOne(product.code)}>Elimina</button>
                          </>
                        ) : (
                          <button className="link-button" disabled={product.availableQuantity === 0 || product.discontinued} onClick={() => onAddToCart?.(product)}>Aggiungi</button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
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

function getStockStatus(product: Product) {
  if (product.discontinued) return { label: 'Disattivato', className: 'out' };
  if (product.availableQuantity === 0) return { label: 'Esaurito', className: 'out' };
  if (product.availableQuantity <= 3) return { label: 'Scorta bassa', className: 'low' };
  return { label: 'Disponibile', className: 'ok' };
}
