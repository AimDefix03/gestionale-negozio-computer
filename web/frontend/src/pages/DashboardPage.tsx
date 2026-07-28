import { Order, StockMovement } from '../api';
import SimpleList from '../components/common/SimpleList';
import StatCard from '../components/common/StatCard';
import { DashboardStats } from '../types/ui';
import { money } from '../utils/formatters';

type Props = {
  stats: DashboardStats;
  recentOrders: Order[];
  recentMovements: StockMovement[];
};

export default function DashboardPage({ stats, recentOrders, recentMovements }: Props) {
  return (
    <>
      <section className="stats-grid">
        <StatCard label="Prodotti" value={String(stats.products)} caption="Elementi catalogo" />
        <StatCard label="Inventario" value={money.format(stats.inventoryValue)} caption="Valore scontato" />
        <StatCard label="Scorte basse" value={String(stats.lowStock)} caption="Disponibile entro 3 unita" />
        <StatCard label="Ordini" value={String(stats.orders)} caption={money.format(stats.revenue)} />
      </section>
      <div className="content-grid two">
        <SimpleList title="Ultimi ordini" items={recentOrders.map((order) => `${order.code} - ${order.customer} - ${money.format(order.total)}`)} />
        <SimpleList title="Movimenti recenti" items={recentMovements.map((movement) => `${movement.typeLabel} ${movement.productCode} - ${movement.quantity} unita`)} />
      </div>
    </>
  );
}
