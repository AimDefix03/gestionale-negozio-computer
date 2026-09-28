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
        <StatCard label="Valore potenziale" value={money.format(stats.potentialRetailStockValue)} caption="Quantita per prezzo vendita scontato" />
        <StatCard label="Valore noto a costo" value={money.format(stats.knownInventoryCostValue)} caption={`${stats.costedUnits} unita valorizzate`} />
        <StatCard label="Copertura costo" value={`${stats.costCoveragePercentage.toFixed(2)}%`} caption={`${stats.uncostedUnits} unita senza costo documentato`} />
        <StatCard label="Margine potenziale" value={money.format(stats.potentialGrossMarginOnCostedStock)} caption="Solo quota valorizzata, dato gestionale" />
        <StatCard label="Scorte basse" value={String(stats.lowStock)} caption="Disponibile entro 3 unita" />
        <StatCard label="Ordini in bozza" value={String(stats.orders.draftOrders)} caption={money.format(stats.orders.draftOrderValue)} />
        <StatCard label="Ordini confermati" value={String(stats.orders.confirmedOrders)} caption={money.format(stats.orders.confirmedOrderValue)} />
        <StatCard label="Ordini evasi" value={String(stats.orders.fulfilledOrders)} caption={money.format(stats.orders.fulfilledOrderValue)} />
        <StatCard label="Incassato lordo" value={money.format(stats.orders.grossCollected)} caption={`${money.format(stats.orders.refunded)} rimborsato o stornato`} />
        <StatCard label="Incassato netto" value={money.format(stats.orders.netCollected)} caption={`${stats.orders.totalOrders} ordini totali · ${stats.orders.canceledOrders} annullati`} />
      </section>
      <div className="content-grid two">
        <SimpleList title="Ultimi ordini" items={recentOrders.map((order) => `${order.code} - ${order.customer} - ${money.format(order.total)}`)} />
        <SimpleList title="Movimenti recenti" items={recentMovements.map((movement) => `${movement.typeLabel} ${movement.productCode} - ${movement.quantity} unita`)} />
      </div>
    </>
  );
}
