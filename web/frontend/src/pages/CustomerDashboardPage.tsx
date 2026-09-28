import { CustomerDashboardSummary } from '../api';
import SimpleList from '../components/common/SimpleList';
import StatCard from '../components/common/StatCard';
import { money } from '../utils/formatters';

type Props = {
  dashboard: CustomerDashboardSummary;
};

export default function CustomerDashboardPage({ dashboard }: Props) {
  return (
    <>
      <section className="stats-grid customer-stats-grid">
        <StatCard label="I miei ordini" value={String(dashboard.totalOrders)} caption="Totale personale" />
        <StatCard label="Bozze" value={String(dashboard.draftOrders)} caption="Da confermare" />
        <StatCard label="In lavorazione" value={String(dashboard.confirmedOrders)} caption="Ordini confermati" />
        <StatCard label="Completati" value={String(dashboard.fulfilledOrders)} caption="Ordini evasi" />
        <StatCard label="Annullati" value={String(dashboard.canceledOrders)} caption="Ordini cancellati" />
      </section>
      <div className="content-grid">
        <SimpleList
          title="I miei ordini recenti"
          items={dashboard.recentOrders.map((order) => `${order.code} · ${order.statusLabel} · ${order.paymentStatusLabel} · ${money.format(order.total)}`)}
        />
      </div>
    </>
  );
}
