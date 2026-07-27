import { useState } from 'react';
import type {
  InventoryReport,
  InventoryReportQuery,
  ReportFormat,
  SalesReport,
  SalesReportQuery
} from '../api';
import StatCard from '../components/common/StatCard';
import { dateTime, money } from '../utils/formatters';

type Props = {
  sales: SalesReport | null;
  inventory: InventoryReport | null;
  salesQuery: SalesReportQuery;
  inventoryQuery: InventoryReportQuery;
  busy: boolean;
  onSalesQueryChange: (query: SalesReportQuery) => void;
  onInventoryQueryChange: (query: InventoryReportQuery) => void;
  onExportSales: (format: ReportFormat) => void;
  onExportInventory: (format: ReportFormat) => void;
  onRefreshSales: () => void;
  onRefreshInventory: () => void;
};

export default function ReportsPage(props: Props) {
  const [activeReport, setActiveReport] = useState<'sales' | 'inventory'>('sales');
  const isSales = activeReport === 'sales';

  return (
    <div className="reports-page">
      <section className="report-switcher" aria-label="Tipo di report">
        <button className={isSales ? 'active' : ''} type="button" onClick={() => setActiveReport('sales')}>Vendite</button>
        <button className={!isSales ? 'active' : ''} type="button" onClick={() => setActiveReport('inventory')}>Magazzino</button>
      </section>
      {isSales ? <SalesView {...props} /> : <InventoryView {...props} />}
    </div>
  );
}

function SalesView(props: Props) {
  const report = props.sales;
  return (
    <>
      <section className="panel report-controls">
        <div className="panel-toolbar">
          <div className="section-heading compact"><span>Analisi commerciale</span><h2>Vendite e incassi</h2><p>Valore ordini, pagamenti, rimborsi e saldo residuo.</p></div>
          <ExportActions busy={props.busy} onExport={props.onExportSales} onRefresh={props.onRefreshSales} />
        </div>
        <div className="list-filters report-filters">
          <label>Dal<input type="date" value={props.salesQuery.from ?? ''} onChange={(event) => props.onSalesQueryChange({ ...props.salesQuery, from: event.target.value || undefined })} /></label>
          <label>Al<input type="date" value={props.salesQuery.to ?? ''} onChange={(event) => props.onSalesQueryChange({ ...props.salesQuery, to: event.target.value || undefined })} /></label>
          <label>Stato<select value={props.salesQuery.status ?? 'ALL'} onChange={(event) => props.onSalesQueryChange({ ...props.salesQuery, status: event.target.value as SalesReportQuery['status'] })}><option value="ALL">Tutti</option><option value="DRAFT">Bozza</option><option value="CONFIRMED">Confermato</option><option value="FULFILLED">Evaso</option><option value="CANCELED">Annullato</option></select></label>
        </div>
        {report && <ReportTimestamp generatedAt={report.generatedAt} caption={`${report.from} - ${report.to} · ${report.statusLabel}`} />}
      </section>
      {report ? (
        <>
          <section className="report-metrics">
            <StatCard label="Ordini" value={String(report.orderCount)} caption={money.format(report.orderValue)} />
            <StatCard label="Incassato" value={money.format(report.paidAmount)} caption={`Netto ${money.format(report.netCollectedAmount)}`} />
            <StatCard label="Rimborsato" value={money.format(report.refundedAmount)} caption="Importi restituiti" />
            <StatCard label="Residuo" value={money.format(report.outstandingAmount)} caption={`Media ${money.format(report.averageOrderValue)}`} />
          </section>
          <div className="report-layout">
            <section className="panel report-table-panel">
              <div className="section-heading compact"><span>Dettaglio</span><h2>Ordini nel periodo</h2></div>
              <div className="table-shell report-table-shell">
                <table>
                  <thead><tr><th>Ordine</th><th>Data</th><th>Cliente</th><th>Stato</th><th>Valore</th><th>Incassato</th><th>Rimborsato</th><th>Residuo</th></tr></thead>
                  <tbody>
                    {report.orders.map((order) => <tr key={order.code}><td className="strong">{order.code}</td><td>{dateTime.format(new Date(order.timestamp))}</td><td>{order.customer}</td><td><span className={`report-status ${order.status.toLowerCase()}`}>{order.statusLabel}</span></td><td>{money.format(order.total)}</td><td>{money.format(order.paidAmount)}</td><td>{money.format(order.refundedAmount)}</td><td>{money.format(order.outstandingAmount)}</td></tr>)}
                    {report.orders.length === 0 && <tr><td colSpan={8} className="empty-table-cell">Nessun ordine per i filtri selezionati.</td></tr>}
                  </tbody>
                </table>
              </div>
            </section>
            <section className="panel report-ranking">
              <div className="section-heading compact"><span>Composizione</span><h2>Prodotti principali</h2></div>
              <ol>
                {report.topProducts.map((product) => <li key={product.productCode}><div><strong>{product.productName}</strong><span>{product.productCode} · {product.quantity} unita</span></div><b>{money.format(product.orderValue)}</b></li>)}
                {report.topProducts.length === 0 && <li className="report-empty">Nessun prodotto da aggregare.</li>}
              </ol>
            </section>
          </div>
        </>
      ) : <ReportLoading />}
    </>
  );
}

function InventoryView(props: Props) {
  const report = props.inventory;
  const lifecycle = props.inventoryQuery.discontinued === undefined ? 'ALL' : props.inventoryQuery.discontinued ? 'DISCONTINUED' : 'ACTIVE';
  return (
    <>
      <section className="panel report-controls">
        <div className="panel-toolbar">
          <div className="section-heading compact"><span>Controllo scorte</span><h2>Snapshot di magazzino</h2><p>Giacenza fisica, riservata, disponibile e valore corrente.</p></div>
          <ExportActions busy={props.busy} onExport={props.onExportInventory} onRefresh={props.onRefreshInventory} />
        </div>
        <div className="list-filters report-filters inventory-report-filters">
          <label className="search-field">Cerca<input value={props.inventoryQuery.q ?? ''} placeholder="Codice, nome, brand o tipo" onChange={(event) => props.onInventoryQueryChange({ ...props.inventoryQuery, q: event.target.value || undefined })} /></label>
          <label>Categoria<select value={props.inventoryQuery.category ?? 'ALL'} onChange={(event) => props.onInventoryQueryChange({ ...props.inventoryQuery, category: event.target.value as InventoryReportQuery['category'] })}><option value="ALL">Tutte</option><option value="HARDWARE">Hardware</option><option value="SOFTWARE">Software</option></select></label>
          <label>Stock<select value={props.inventoryQuery.stock ?? 'ALL'} onChange={(event) => props.onInventoryQueryChange({ ...props.inventoryQuery, stock: event.target.value as InventoryReportQuery['stock'] })}><option value="ALL">Tutto</option><option value="AVAILABLE">Disponibile</option><option value="LOW">Scorta bassa</option><option value="OUT">Esaurito</option></select></label>
          <label>Prodotto<select value={lifecycle} onChange={(event) => props.onInventoryQueryChange({ ...props.inventoryQuery, discontinued: event.target.value === 'ALL' ? undefined : event.target.value === 'DISCONTINUED' })}><option value="ALL">Tutti</option><option value="ACTIVE">Attivi</option><option value="DISCONTINUED">Disattivati</option></select></label>
        </div>
        {report && <ReportTimestamp generatedAt={report.generatedAt} caption={`${report.productCount} prodotti nel risultato`} />}
      </section>
      {report ? (
        <>
          <section className="report-metrics">
            <StatCard label="Valore stock" value={money.format(report.inventoryValue)} caption={`${report.productCount} prodotti`} />
            <StatCard label="Disponibili" value={String(report.availableUnits)} caption={`${report.physicalUnits} unita fisiche`} />
            <StatCard label="Riservate" value={String(report.reservedUnits)} caption="Impegnate da ordini" />
            <StatCard label="Da presidiare" value={String(report.lowStockCount + report.outOfStockCount)} caption={`${report.lowStockCount} basse · ${report.outOfStockCount} esaurite`} />
          </section>
          <section className="panel report-table-panel">
            <div className="section-heading compact"><span>Inventario</span><h2>Posizione articoli</h2></div>
            <div className="table-shell report-table-shell inventory-report-table">
              <table>
                <thead><tr><th>Prodotto</th><th>Classificazione</th><th>Fisico</th><th>Riservato</th><th>Disponibile</th><th>Prezzo netto</th><th>Valore stock</th><th>Stato</th></tr></thead>
                <tbody>
                  {report.products.map((product) => <tr key={product.code}><td><div className="product-cell"><strong>{product.name}</strong><span>{product.code} · {product.brand}</span></div></td><td><div className="product-cell"><strong>{product.productType}</strong><span>{product.categoryLabel}</span></div></td><td>{product.quantity}</td><td>{product.reservedQuantity}</td><td>{product.availableQuantity}</td><td>{money.format(product.discountedPrice)}</td><td>{money.format(product.stockValue)}</td><td><span className={`report-status ${product.discontinued ? 'canceled' : product.stockStatus.toLowerCase()}`}>{product.discontinued ? 'Disattivato' : product.stockStatusLabel}</span></td></tr>)}
                  {report.products.length === 0 && <tr><td colSpan={8} className="empty-table-cell">Nessun prodotto per i filtri selezionati.</td></tr>}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : <ReportLoading />}
    </>
  );
}

function ExportActions({ busy, onExport, onRefresh }: { busy: boolean; onExport: (format: ReportFormat) => void; onRefresh: () => void }) {
  return <div className="toolbar-actions report-actions"><button className="button secondary compact-button" type="button" disabled={busy} onClick={onRefresh}>Aggiorna</button><button className="button secondary compact-button" type="button" disabled={busy} onClick={() => onExport('CSV')}>CSV</button><button className="button secondary compact-button" type="button" disabled={busy} onClick={() => onExport('XLSX')}>Excel</button><button className="button primary compact-button" type="button" disabled={busy} onClick={() => onExport('PDF')}>PDF</button></div>;
}

function ReportTimestamp({ generatedAt, caption }: { generatedAt: string; caption: string }) {
  return <div className="report-timestamp"><span>Aggiornato {dateTime.format(new Date(generatedAt))}</span><strong>{caption}</strong></div>;
}

function ReportLoading() {
  return <section className="panel report-loading" aria-live="polite">Caricamento report...</section>;
}
