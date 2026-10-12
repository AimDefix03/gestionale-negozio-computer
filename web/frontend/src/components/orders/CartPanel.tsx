import { PaymentMethod } from '../../api';
import { CartItem } from '../../types/ui';
import { money } from '../../utils/formatters';

type Props = {
  cart: CartItem[];
  draftDirty?: boolean;
  customerLabel: string;
  customerReady: boolean;
  paymentMethod: PaymentMethod;
  busy: boolean;
  onPaymentMethodChange: (method: PaymentMethod) => void;
  onCheckout: () => void;
  onClear: () => void;
  onQuantityChange: (productCode: string, quantity: number) => void;
};

export default function CartPanel({ cart, draftDirty = false, customerLabel, customerReady, paymentMethod, busy, onPaymentMethodChange, onCheckout, onClear, onQuantityChange }: Props) {
  const total = cart.reduce((sum, item) => sum + item.product.discountedPrice * item.quantity, 0);
  return (
    <aside className="panel">
      <div className="section-heading compact"><span>Riepilogo</span><h2>Bozza ordine</h2><p>Controlla cliente, righe, prezzi e pagamento prima di creare la bozza.</p></div>
      <div className={`sales-customer-summary ${customerReady ? 'ready' : ''}`}><span>Cliente</span><strong>{customerLabel}</strong></div>
      {cart.length ? <ul className="cart-lines">{cart.map((item) => <li key={item.product.code}>
        <div><strong>{item.product.name}</strong><span>{item.product.code} · {money.format(item.product.discountedPrice)} cad.</span></div>
        <div className="quantity-stepper">
          <button type="button" aria-label={`Riduci quantita ${item.product.name}`} disabled={busy} onClick={() => onQuantityChange(item.product.code, item.quantity - 1)}>−</button>
          <strong>{item.quantity}</strong>
          <button type="button" aria-label={`Aumenta quantita ${item.product.name}`} disabled={busy || (item.maximumQuantity !== undefined && item.quantity >= item.maximumQuantity)} onClick={() => onQuantityChange(item.product.code, item.quantity + 1)}>+</button>
        </div>
        <strong>{money.format(item.product.discountedPrice * item.quantity)}</strong>
      </li>)}</ul> : <div className="empty-state">Nessun prodotto selezionato.</div>}
      <label className="checkout-payment">Metodo di pagamento
        <select value={paymentMethod} disabled={busy} onChange={(event) => onPaymentMethodChange(event.target.value as PaymentMethod)}>
          <option value="CARD">Carta</option>
          <option value="BANK_TRANSFER">Bonifico bancario</option>
          <option value="CASH">Contanti</option>
        </select>
      </label>
      <div className="sales-total"><span>Totale ordine</span><strong>{money.format(total)}</strong></div>
      <div className="form-actions">{draftDirty && <span className="draft-status" role="status">Bozza salvata per questa sessione</span>}<button className="button secondary" disabled={busy || cart.length === 0} onClick={onClear}>Svuota</button><button className="button primary" disabled={busy || cart.length === 0 || !customerReady} onClick={onCheckout}>{busy ? 'Registrazione...' : 'Crea bozza ordine'}</button></div>
    </aside>
  );
}
