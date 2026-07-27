import { PaymentMethod } from '../../api';
import { CartItem } from '../../types/ui';
import { money } from '../../utils/formatters';

type Props = {
  cart: CartItem[];
  paymentMethod: PaymentMethod;
  onPaymentMethodChange: (method: PaymentMethod) => void;
  onCheckout: () => void;
  onClear: () => void;
};

export default function CartPanel({ cart, paymentMethod, onPaymentMethodChange, onCheckout, onClear }: Props) {
  const total = cart.reduce((sum, item) => sum + item.product.discountedPrice * item.quantity, 0);
  return (
    <aside className="panel">
      <div className="section-heading compact"><span>Cliente</span><h2>Carrello</h2><p>Prodotti selezionati dal catalogo.</p></div>
      {cart.length ? <ul className="clean-list">{cart.map((item) => <li key={item.product.code}>{item.product.name} x {item.quantity}</li>)}</ul> : <div className="empty-state">Carrello vuoto.</div>}
      <label className="checkout-payment">Metodo di pagamento
        <select value={paymentMethod} onChange={(event) => onPaymentMethodChange(event.target.value as PaymentMethod)}>
          <option value="CARD">Carta</option>
          <option value="BANK_TRANSFER">Bonifico bancario</option>
          <option value="CASH">Contanti</option>
        </select>
      </label>
      <div className="form-actions"><strong>{money.format(total)}</strong><button className="button secondary" onClick={onClear}>Svuota</button><button className="button primary" disabled={cart.length === 0} onClick={onCheckout}>Crea ordine</button></div>
    </aside>
  );
}
