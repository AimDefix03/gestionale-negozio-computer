import { Order, Product, StockMovement } from '../../api';
import { money } from '../../utils/formatters';

type Props = {
  product: Product | null;
  movements: StockMovement[];
  orders: Order[];
  canEdit: boolean;
  canMove: boolean;
  onEdit: (product: Product) => void;
  onMovement: (product: Product) => void;
  onAddToCart?: (product: Product) => void;
};

export default function ProductInsightPanel({ product, movements, orders, canEdit, canMove, onEdit, onMovement, onAddToCart }: Props) {
  if (!product) {
    return <section className="panel product-insight"><div className="section-heading compact"><span>Dettaglio</span><h2>Nessun prodotto selezionato</h2><p>Seleziona un prodotto dal catalogo per aprire la scheda operativa.</p></div></section>;
  }

  const status = productStatus(product);
  const inventoryValue = product.discountedPrice * product.quantity;
  const orderLines = orders.flatMap((order) => order.items
    .filter((item) => item.productCode === product.code)
    .map((item) => `${order.code} - ${order.customer} - ${item.quantity} unita - ${money.format(item.lineTotal)}`)
  );

  return (
    <section className="panel product-insight">
      <div className="section-heading compact">
        <span>Dettaglio prodotto</span><h2>{product.name}</h2><p>{product.code} · {product.brand} · {product.productType}</p>
      </div>
      <div className="insight-summary"><span className={`status-badge ${status.className}`}>{status.label}</span><strong>{money.format(inventoryValue)}</strong><small>Valore giacenza</small></div>
      <div className="insight-grid">
        <div><span>Categoria</span><strong>{product.category}</strong></div>
        <div><span>Giacenza</span><strong>{product.quantity}</strong></div>
        <div><span>Riservato</span><strong>{product.reservedQuantity}</strong></div>
        <div><span>Disponibile</span><strong>{product.availableQuantity}</strong></div>
        <div><span>Prezzo</span><strong>{money.format(product.discountedPrice)}</strong></div>
        <div><span>Sconto</span><strong>{product.discount}%</strong></div>
      </div>
      {product.usageContext && <p className="detail-note">Utilizzo: {product.usageContext}</p>}
      <p className="detail-note">{product.description}</p>
      <div className="form-actions insight-actions">
        {canEdit || canMove ? (
          <>{canEdit && <button className="button secondary" onClick={() => onEdit(product)}>Modifica</button>}{canMove && <button className="button primary" onClick={() => onMovement(product)}>Movimento</button>}</>
        ) : <button className="button primary" disabled={product.availableQuantity === 0 || product.discontinued} onClick={() => onAddToCart?.(product)}>Aggiungi al carrello</button>}
      </div>
      <div className="insight-section">
        <h3>Movimenti recenti</h3>
        {movements.length ? <ul className="compact-feed">{movements.slice(0, 4).map((movement) => <li key={movement.id}><strong>{movement.typeLabel}</strong><span>{movement.quantity} unita · {movement.previousQuantity} → {movement.newQuantity}</span></li>)}</ul> : <div className="empty-state compact-empty">Nessun movimento registrato.</div>}
      </div>
      <div className="insight-section">
        <h3>Ordini collegati</h3>
        {orderLines.length ? <ul className="compact-feed">{orderLines.slice(0, 4).map((line) => <li key={line}><span>{line}</span></li>)}</ul> : <div className="empty-state compact-empty">Nessun ordine collegato.</div>}
      </div>
    </section>
  );
}

function productStatus(product: Product) {
  if (product.discontinued) return { label: 'Disattivato', className: 'out' };
  if (product.availableQuantity === 0) return { label: 'Esaurito', className: 'out' };
  if (product.availableQuantity <= 3) return { label: 'Scorta bassa', className: 'low' };
  return { label: 'Disponibile', className: 'ok' };
}
