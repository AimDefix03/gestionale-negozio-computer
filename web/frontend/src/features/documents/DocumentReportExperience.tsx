import DocumentsPage from '../../pages/DocumentsPage';
import ReportsPage from '../../pages/ReportsPage';
import { errorMessage } from '../../utils/errors';
import type { DocumentReportFlowController } from './useDocumentReportFlow';

type Props = {
  view: 'documents' | 'reports';
  flow: DocumentReportFlowController;
  busy: boolean;
};

export default function DocumentReportExperience({ view, flow, busy }: Props) {
  const retryDocuments = () => void flow.refreshDocuments().catch(() => undefined);
  const retrySales = () => void flow.refreshSales().catch(() => undefined);
  const retryInventory = () => void flow.refreshInventory().catch(() => undefined);

  if (view === 'documents') {
    if (flow.documentLoading && flow.documentPage.content.length === 0) {
      return <ResourceState area="Documenti" title="Caricamento documenti" message="Sto recuperando fatture e note di credito simulate." />;
    }
    if (flow.documentError && flow.documentPage.content.length === 0) {
      return <ResourceState area="Documenti" title="Documenti non disponibili" message={errorMessage(flow.documentError)} onRetry={retryDocuments} />;
    }

    return (
      <>
        {flow.documentError && (
          <ResourceState
            compact
            area="Documenti"
            title="Aggiornamento non riuscito"
            message={`${errorMessage(flow.documentError)} I documenti gia caricati restano disponibili.`}
            onRetry={retryDocuments}
          />
        )}
        <DocumentsPage
          page={flow.documentPage}
          query={flow.documentQuery}
          creditReason={flow.creditReason}
          busy={busy || flow.documentRefreshing}
          pageSize={flow.pageSize}
          onQueryChange={flow.setDocumentQuery}
          onCreditReasonChange={flow.setCreditReason}
          onCreditNote={(orderCode) => void flow.createCreditNoteForOrder(orderCode)}
        />
      </>
    );
  }

  return (
    <ReportsPage
      sales={flow.salesReport}
      inventory={flow.inventoryReport}
      salesQuery={flow.salesQuery}
      inventoryQuery={flow.inventoryQuery}
      salesLoading={flow.salesLoading}
      salesRefreshing={flow.salesRefreshing}
      salesError={flow.salesError ? errorMessage(flow.salesError) : ''}
      inventoryLoading={flow.inventoryLoading}
      inventoryRefreshing={flow.inventoryRefreshing}
      inventoryError={flow.inventoryError ? errorMessage(flow.inventoryError) : ''}
      busy={busy || flow.exporting}
      onSalesQueryChange={flow.setSalesQuery}
      onInventoryQueryChange={flow.setInventoryQuery}
      onExportSales={(format) => void flow.exportReport('sales', format)}
      onExportInventory={(format) => void flow.exportReport('inventory', format)}
      onRefreshSales={retrySales}
      onRefreshInventory={retryInventory}
    />
  );
}

type ResourceStateProps = {
  area: string;
  title: string;
  message: string;
  compact?: boolean;
  onRetry?: () => void;
};

function ResourceState({ area, title, message, compact = false, onRetry }: ResourceStateProps) {
  return (
    <section className={`panel catalog-resource-state${compact ? ' compact' : ''}`} role={compact ? 'alert' : 'status'}>
      <div className="section-heading compact">
        <span>{area}</span>
        <h2>{title}</h2>
        <p>{message}</p>
      </div>
      {onRetry && <button className="button secondary" type="button" onClick={onRetry}>Riprova</button>}
    </section>
  );
}
