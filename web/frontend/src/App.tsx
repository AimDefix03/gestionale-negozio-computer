import { FormEvent, useEffect, useState } from 'react';
import {
  createInitialBalance,
  createMovement,
  CustomerDashboardSummary,
  DashboardSummary,
  fetchCustomerDashboard,
  fetchDashboard,
  fetchMovementPage,
  fetchInventoryReconciliation,
  InventoryReconciliation,
  MovementQuery,
  PageResponse,
  SessionExpiredError,
  StockMovement,
  UserPermission
} from './api';
import WorkspaceChrome from './components/layout/WorkspaceChrome';
import UnsavedChangesDialog from './components/common/UnsavedChangesDialog';
import { useDraftStore } from './drafts/DraftStoreProvider';
import AdministrationExperience from './features/administration/AdministrationExperience';
import useAdministrationFlow from './features/administration/useAdministrationFlow';
import CatalogExperience from './features/catalog/CatalogExperience';
import useCatalogFlow from './features/catalog/useCatalogFlow';
import DocumentReportExperience from './features/documents/DocumentReportExperience';
import useDocumentReportFlow from './features/documents/useDocumentReportFlow';
import OrderExperience from './features/orders/OrderExperience';
import useOrderFlow from './features/orders/useOrderFlow';
import PartnerExperience from './features/partners/PartnerExperience';
import usePartnerFlow from './features/partners/usePartnerFlow';
import PurchaseExperience from './features/purchases/PurchaseExperience';
import usePurchaseFlow from './features/purchases/usePurchaseFlow';
import usePhysicalInventoryFlow from './features/inventory/usePhysicalInventoryFlow';
import { SessionEntry, SessionOverlays } from './features/session/SessionExperience';
import useSessionFlow from './features/session/useSessionFlow';
import useCommandExecution, { type UiNotice } from './hooks/useCommandExecution';
import useDraftState from './hooks/useDraftState';
import useWorkspaceNavigation from './hooks/useWorkspaceNavigation';
import CustomerDashboardPage from './pages/CustomerDashboardPage';
import DashboardPage from './pages/DashboardPage';
import InventoryPage from './pages/InventoryPage';
import { MenuGroup, MenuItem, MovementFormState, View } from './types/ui';
import { errorMessage } from './utils/errors';
import { titleForView } from './utils/formatters';
import { upsertPageItem } from './utils/pageState';

const DEFAULT_PAGE_SIZE = 8;
const INITIAL_MOVEMENT_FORM: MovementFormState = { productCode: '', operation: 'INITIAL_BALANCE', quantity: '0', reason: '' };

type PendingNavigation = {
  view: View;
  title: string;
  message: string;
  confirmLabel: string;
  destructive?: boolean;
  action: () => void;
};

function emptyPage<T>(size = DEFAULT_PAGE_SIZE): PageResponse<T> {
  return { content: [], page: 0, size, totalElements: 0, totalPages: 0, first: true, last: true };
}

export default function App() {
  const { activeView, openTabs, openMenu, setActiveView, setOpenMenu, openTab, closeTab, resetNavigation } = useWorkspaceNavigation();
  const drafts = useDraftStore();
  const [pendingNavigation, setPendingNavigation] = useState<PendingNavigation | null>(null);
  const [notice, setNotice] = useState<UiNotice | null>(null);
  const sessionFlow = useSessionFlow({
    onAuthenticated: resetNavigation,
    onSignedOut: resetWorkspaceState,
    onNotice: (message, title) => setWorkspaceNotice(message, 'success', title)
  });
  const { currentUser, sessionExpiresAt } = sessionFlow;
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null);
  const [customerDashboard, setCustomerDashboard] = useState<CustomerDashboardSummary | null>(null);
  const [movementQuery, setMovementQuery] = useState<MovementQuery>({ page: 0, size: DEFAULT_PAGE_SIZE });
  const [movementPage, setMovementPage] = useState<PageResponse<StockMovement>>(emptyPage<StockMovement>());
  const [inventoryReconciliation, setInventoryReconciliation] = useState<InventoryReconciliation | null>(null);
  const { value: movementForm, setValue: setMovementForm, dirty: movementFormDirty, clear: clearMovementDraft } = useDraftState({
    key: `inventory:${currentUser?.id ?? 'anonymous'}`,
    view: 'inventory',
    label: 'Comando di magazzino',
    initialValue: INITIAL_MOVEMENT_FORM,
    enabled: Boolean(currentUser)
  });
  const [busy, setBusy] = useState(false);
  const { executeCommand, commandBusy } = useCommandExecution({ onNotice: setNotice, onCommandError: handleCommandError });
  const catalogFlow = useCatalogFlow({
    enabled: Boolean(currentUser),
    customer: currentUser?.role === 'CUSTOMER',
    executeCommand,
    onRefreshDashboard: loadRoleDashboard
  });
  const { productLookup } = catalogFlow;
  const orderFlow = useOrderFlow({
    enabled: Boolean(currentUser),
    currentUser,
    executeCommand,
    onRefreshDashboard: loadRoleDashboard,
    onRefreshInventory: refreshInventoryAfterOrder,
    onOpenOrders: () => requestOpenTab('orders'),
    onNotice: setWorkspaceNotice
  });
  const partnerFlow = usePartnerFlow({
    enabled: Boolean(currentUser?.permissions.includes('VIEW_PARTNERS')),
    canLinkAccounts: Boolean(currentUser?.permissions.includes('MANAGE_ACCOUNTS')),
    executeCommand
  });
  const purchaseFlow = usePurchaseFlow({
    enabled: Boolean(currentUser?.permissions.includes('VIEW_PURCHASE_ORDERS')),
    accountId: currentUser?.id,
    executeCommand,
    onInventoryChanged: refreshInventoryAfterPurchase
  });
  const uiBusy = busy || commandBusy;

  const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN';
  const hasPermission = (permission: UserPermission) => Boolean(currentUser?.permissions.includes(permission));
  const canManageProducts = hasPermission('MANAGE_PRODUCTS');
  const canCreateOrders = orderFlow.canCreateOrders;
  const canViewPartners = hasPermission('VIEW_PARTNERS');
  const canManagePartners = hasPermission('MANAGE_PARTNERS');
  const canManageInventory = hasPermission('MANAGE_INVENTORY');
  const canViewPurchases = hasPermission('VIEW_PURCHASE_ORDERS');
  const canManagePurchases = hasPermission('MANAGE_PURCHASE_ORDERS');
  const canManageDocuments = hasPermission('MANAGE_DOCUMENTS');
  const canManageAccounts = hasPermission('MANAGE_ACCOUNTS');
  const canManageCompanySettings = hasPermission('MANAGE_COMPANY_SETTINGS');
  const canViewAudit = hasPermission('VIEW_AUDIT');
  const canViewReports = hasPermission('VIEW_REPORTS');
  const physicalInventoryFlow = usePhysicalInventoryFlow({
    enabled: Boolean(currentUser && canManageInventory),
    executeCommand,
    onInventoryChanged: refreshInventoryData
  });
  const documentReportFlow = useDocumentReportFlow({
    documentsEnabled: Boolean(currentUser && canManageDocuments),
    reportsEnabled: Boolean(currentUser && canViewReports),
    reportsActive: activeView === 'reports',
    executeCommand,
    onOpenDocuments: () => requestOpenTab('documents'),
    onNotice: setWorkspaceNotice,
    onError: handleCommandError
  });
  const administrationFlow = useAdministrationFlow({
    enabled: Boolean(currentUser),
    currentUser,
    canManageAccounts,
    canManageCompanySettings,
    canViewAudit,
    monitoringActive: activeView === 'monitoring',
    executeCommand,
    onAccountApplied: partnerFlow.applyCustomerAccount,
    onRefreshCustomerAccounts: partnerFlow.refreshCustomerAccounts,
    onNotice: setWorkspaceNotice
  });
  useEffect(() => {
    if (currentUser) {
      void loadWorkspace();
    }
  }, [currentUser?.id]);

  useEffect(() => {
    if (currentUser && canManageInventory) void loadMovementPage();
  }, [currentUser, canManageInventory, movementQuery]);

  async function loadWorkspace() {
    setNotice(null);
    await Promise.all([loadWorkspaceSnapshot(), loadPagedViews()]);
  }

  async function loadWorkspaceSnapshot() {
    await loadRoleDashboard();
  }

  async function loadDashboard() {
    setDashboard(await fetchDashboard());
    setCustomerDashboard(null);
  }

  async function loadCustomerDashboard() {
    setCustomerDashboard(await fetchCustomerDashboard());
    setDashboard(null);
  }

  async function loadRoleDashboard() {
    if (currentUser?.role === 'CUSTOMER') {
      await loadCustomerDashboard();
      return;
    }
    await loadDashboard();
  }

  async function refreshInventoryAfterPurchase() {
    const requests: Promise<unknown>[] = [catalogFlow.refreshProducts(), loadDashboard()];
    if (canManageInventory) requests.push(loadMovementPage(), loadInventoryReconciliation());
    await Promise.all(requests);
  }

  async function loadPagedViews() {
    await Promise.all([
      canManageInventory ? loadMovementPage() : Promise.resolve(setMovementPage(emptyPage<StockMovement>(movementQuery.size))),
      canManageInventory ? loadInventoryReconciliation() : Promise.resolve(setInventoryReconciliation(null))
    ]);
  }

  async function loadMovementPage() {
    setMovementPage(await fetchMovementPage(movementQuery));
  }

  async function loadInventoryReconciliation() {
    setInventoryReconciliation(await fetchInventoryReconciliation());
  }

  async function refreshInventoryData() {
    await Promise.all([catalogFlow.refreshProducts(), loadMovementPage(), loadInventoryReconciliation(), loadDashboard()]);
  }

  async function refreshInventoryAfterOrder() {
    const requests: Promise<unknown>[] = [catalogFlow.refreshProducts()];
    if (currentUser?.role !== 'CUSTOMER' && canManageInventory) requests.push(loadMovementPage(), loadInventoryReconciliation());
    await Promise.all(requests);
  }

  function applyMovement(movement: StockMovement) {
    setMovementPage((page) => upsertPageItem(page, movement, (item) => item.id));
    catalogFlow.applyInventoryMovement(movement);
  }

  function setWorkspaceNotice(message: string, tone: UiNotice['tone'] = 'info', title = tone === 'error' ? 'Operazione non riuscita' : 'Informazione') {
    setNotice({ tone, title, message });
  }

  async function handleMovement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentUser) return;
    const submittedForm = { ...movementForm };
    const quantity = Number(submittedForm.quantity);
    await executeCommand({
      key: `inventory:${submittedForm.operation}:${submittedForm.productCode}`,
      command: () => submittedForm.operation === 'INITIAL_BALANCE'
        ? createInitialBalance({ productCode: submittedForm.productCode, quantity, reason: submittedForm.reason })
        : createMovement({ productCode: submittedForm.productCode, type: submittedForm.operation, quantity, reason: submittedForm.reason }),
      applyResponse: applyMovement,
      afterConfirmed: clearMovementDraft,
      refresh: refreshInventoryData,
      successMessage: `Movimento registrato per ${submittedForm.productCode}.`
    });
  }

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setNotice(null);
    try {
      await action();
    } catch (exception) {
      if (exception instanceof SessionExpiredError && currentUser) {
        return;
      }
      setWorkspaceNotice(errorMessage(exception), 'error');
    } finally {
      setBusy(false);
    }
  }

  function handleCommandError(exception: unknown) {
    if (exception instanceof SessionExpiredError && currentUser) {
      return;
    }
    setWorkspaceNotice(errorMessage(exception), 'error');
  }

  async function handleWorkspaceRefresh() {
    await run(async () => {
      await Promise.all([
        loadWorkspace(),
        catalogFlow.refreshProducts(),
        orderFlow.refreshResources(),
        partnerFlow.refreshResources(),
        canManageInventory ? physicalInventoryFlow.refreshResources() : Promise.resolve(),
        canViewPurchases ? purchaseFlow.refreshResources() : Promise.resolve(),
        documentReportFlow.refreshResources(),
        administrationFlow.refreshResources()
      ]);
      setWorkspaceNotice('I dati del workspace sono stati aggiornati.', 'success', 'Aggiornamento completato');
    });
  }

  function requestOpenTab(target: View) {
    if (target === activeView || !drafts.hasDirtyDrafts(activeView)) {
      openTab(target);
      return true;
    }
    queueDraftGuard(activeView, `Aprire ${titleForView(target)}?`, 'La bozza della scheda corrente resta salvata per questa sessione.', 'Apri scheda', () => {
      openTab(target);
      setOpenMenu(null);
    });
    return false;
  }

  function requestActiveView(target: View) {
    if (target === activeView || !drafts.hasDirtyDrafts(activeView)) {
      setActiveView(target);
      return;
    }
    queueDraftGuard(activeView, `Passare a ${titleForView(target)}?`, 'La bozza della scheda corrente resta salvata per questa sessione.', 'Cambia scheda', () => setActiveView(target));
  }

  function requestCloseTab(target: View) {
    if (!drafts.hasDirtyDrafts(target)) {
      closeTab(target);
      return;
    }
    queueDraftGuard(target, `Chiudere ${titleForView(target)}?`, 'La scheda verra chiusa, ma la bozza restera disponibile quando la riaprirai nella sessione corrente.', 'Chiudi scheda', () => closeTab(target));
  }

  function requestLogout() {
    if (!drafts.hasDirtyDrafts()) {
      void sessionFlow.signOut();
      return;
    }
    const view = drafts.dirtyViews[0] ?? activeView;
    setPendingNavigation({
      view,
      title: 'Terminare la sessione?',
      message: 'Le bozze locali non contengono password, ma verranno eliminate al logout per evitare dati residui sul dispositivo.',
      confirmLabel: 'Elimina bozze ed esci',
      destructive: true,
      action: () => {
        drafts.discardAllDrafts();
        void sessionFlow.signOut();
      }
    });
  }

  function queueDraftGuard(view: View, title: string, message: string, confirmLabel: string, action: () => void) {
    setPendingNavigation({ view, title, message, confirmLabel, action });
  }

  function confirmPendingNavigation() {
    const pending = pendingNavigation;
    setPendingNavigation(null);
    pending?.action();
  }

  function cancelPendingNavigation() {
    setPendingNavigation(null);
  }

  if (!currentUser) {
    return <SessionEntry flow={sessionFlow} />;
  }

  const menuGroups: MenuGroup[] = [
    {
      id: 'workspace',
      label: 'Workspace',
      items: [
        { label: 'Dashboard', description: 'Sintesi operativa del gestionale', view: 'dashboard' },
        { label: 'Catalogo prodotti', description: 'Prodotti, prezzi, brand e disponibilita', view: 'catalog' },
        canViewPartners ? { label: 'Anagrafiche', description: 'Clienti e fornitori strutturati', view: 'partners' as View } : null
      ].filter(Boolean) as MenuItem[]
    },
    {
      id: 'operations',
      label: 'Operazioni',
      items: [
        canCreateOrders ? { label: currentUser.role === 'CUSTOMER' ? 'Nuovo ordine' : 'Vendita', description: currentUser.role === 'CUSTOMER' ? 'Crea una nuova bozza personale' : 'Vendita assistita per clienti censiti o occasionali', view: 'sales' as View } : null,
        canManageInventory ? { label: 'Magazzino', description: 'Carichi, scarichi e movimenti', view: 'inventory' as View } : null,
        { label: 'Ordini', description: 'Acquisti e storico transazioni', view: 'orders' as View },
        canManageDocuments ? { label: 'Documenti', description: 'Fatture e note credito simulate', view: 'documents' as View } : null,
        canViewReports ? { label: 'Report', description: 'Vendite, incassi e situazione magazzino', view: 'reports' as View } : null
      ].filter(Boolean) as MenuItem[]
    },
    ...(canViewPurchases ? [{
      id: 'purchases',
      label: 'Acquisti',
      items: [
        { label: 'Ordini fornitore', description: 'Bozze, invii, consegne attese e residui', view: 'purchases' as View },
        { label: 'Anagrafiche fornitori', description: 'Fornitori e dati di contatto', view: 'partners' as View }
      ] as MenuItem[]
    }] : []),
    ...(canManageAccounts || canManageCompanySettings || canViewAudit ? [{
      id: 'admin',
      label: 'Amministrazione',
      items: [
        canManageAccounts ? { label: 'Account', description: 'Utenti, ruoli e permessi', view: 'accounts' as View } : null,
        canManageCompanySettings ? { label: 'Configurazione azienda', description: 'Dati emittente, IVA e numerazioni', view: 'company' as View } : null,
        canViewAudit ? { label: 'Monitoraggio', description: 'Stato sistema e anomalie recenti', view: 'monitoring' as View } : null,
        canViewAudit ? { label: 'Audit log', description: 'Tracciamento operazioni sensibili', view: 'audit' as View } : null
      ].filter(Boolean) as MenuItem[]
    }] : []),
    {
      id: 'session',
      label: 'Sessione',
      items: [
        { label: 'Aggiorna dati', description: 'Ricarica workspace e dati recenti', action: () => void handleWorkspaceRefresh() },
        { label: 'Cambia password', description: 'Aggiorna credenziali e revoca tutte le sessioni', action: sessionFlow.openPasswordChange },
        { label: 'Logout', description: 'Chiude la sessione corrente', action: requestLogout }
      ] as MenuItem[]
    }
  ];

  return (
    <>
      <WorkspaceChrome
        currentUser={currentUser!}
        sessionExpiresAt={sessionExpiresAt}
        menuGroups={menuGroups}
        openMenu={openMenu}
        openTabs={openTabs}
        dirtyViews={drafts.dirtyViews}
        activeView={activeView}
        notice={notice}
        onMenuChange={setOpenMenu}
        onOpenTab={requestOpenTab}
        onCloseTab={requestCloseTab}
        onActiveViewChange={requestActiveView}
        onRefresh={() => void handleWorkspaceRefresh()}
        onLogout={requestLogout}
      >
        {renderActiveView()}
      </WorkspaceChrome>
      <SessionOverlays flow={sessionFlow} />
      {pendingNavigation && (
        <UnsavedChangesDialog
          title={pendingNavigation.title}
          message={pendingNavigation.message}
          confirmLabel={pendingNavigation.confirmLabel}
          draftLabels={drafts.labelsForView(pendingNavigation.view)}
          destructive={pendingNavigation.destructive}
          onCancel={cancelPendingNavigation}
          onConfirm={confirmPendingNavigation}
        />
      )}
    </>
  );

  function resetWorkspaceState() {
    setNotice(null);
    drafts.discardAllDrafts();
    resetNavigation();
    setMovementQuery({ page: 0, size: DEFAULT_PAGE_SIZE });
    setMovementPage(emptyPage<StockMovement>());
    setInventoryReconciliation(null);
    clearMovementDraft();
    setDashboard(null);
    setCustomerDashboard(null);
    catalogFlow.reset();
    orderFlow.reset();
    partnerFlow.reset();
    purchaseFlow.reset();
    physicalInventoryFlow.reset();
    documentReportFlow.reset();
    administrationFlow.reset();
  }

  function renderActiveView() {
    if (activeView === 'dashboard') return renderDashboard();
    if (activeView === 'catalog') return renderCatalog();
    if (activeView === 'sales') return renderSales();
    if (activeView === 'partners') return renderPartners();
    if (activeView === 'inventory') return renderInventory();
    if (activeView === 'orders') return renderOrders();
    if (activeView === 'purchases') return renderPurchases();
    if (activeView === 'documents') return renderDocuments();
    if (activeView === 'reports') return renderReports();
    if (activeView === 'accounts' || activeView === 'company' || activeView === 'monitoring' || activeView === 'audit') return renderAdministration(activeView);
    return renderDashboard();
  }

  function renderDashboard() {
    if (currentUser?.role === 'CUSTOMER') {
      return customerDashboard
        ? <CustomerDashboardPage dashboard={customerDashboard} />
        : <div className="empty-state">Dashboard personale non disponibile.</div>;
    }
    if (!dashboard) {
      return <div className="empty-state">Dashboard in caricamento. Le aggregazioni saranno mostrate solo dopo la risposta del server.</div>;
    }
    const stats = {
      products: dashboard.products,
      potentialRetailStockValue: dashboard.potentialRetailStockValue,
      knownInventoryCostValue: dashboard.knownInventoryCostValue,
      potentialGrossMarginOnCostedStock: dashboard.potentialGrossMarginOnCostedStock,
      costedUnits: dashboard.costedUnits,
      uncostedUnits: dashboard.uncostedUnits,
      costCoveragePercentage: dashboard.costCoveragePercentage,
      lowStock: dashboard.lowStock,
      outOfStock: dashboard.outOfStock,
      orders: dashboard.orders
    };
    return <DashboardPage stats={stats} recentOrders={dashboard.recentOrders} recentMovements={dashboard.recentMovements} />;
  }

  function renderCatalog() {
    return (
      <CatalogExperience
        flow={catalogFlow}
        customer={currentUser?.role === 'CUSTOMER'}
        busy={uiBusy}
        canManageProducts={Boolean(canManageProducts)}
        canManageInventory={Boolean(canManageInventory)}
        onOpenMovement={(product) => {
          setMovementForm((current) => ({ ...current, productCode: product.code }));
          openTab('inventory');
        }}
      />
    );
  }

  function renderSales() {
    return (
      <OrderExperience
        view="sales"
        flow={orderFlow}
        catalog={catalogFlow}
        currentUser={currentUser!}
        busy={uiBusy}
        onInvoice={(code) => void documentReportFlow.createInvoiceForOrder(code)}
      />
    );
  }

  function renderPartners() {
    return (
      <PartnerExperience
        flow={partnerFlow}
        canManage={Boolean(canManagePartners)}
        canLinkAccounts={Boolean(canManageAccounts)}
        busy={uiBusy}
      />
    );
  }

  function renderInventory() {
    return (
      <InventoryPage
        page={movementPage}
        query={movementQuery}
        form={movementForm}
        formDirty={movementFormDirty}
        products={productLookup}
        reconciliation={inventoryReconciliation}
        busy={uiBusy}
        pageSize={DEFAULT_PAGE_SIZE}
        onQueryChange={setMovementQuery}
        onFormChange={setMovementForm}
        onSubmit={handleMovement}
        physicalInventoryFlow={physicalInventoryFlow}
      />
    );
  }

  function renderOrders() {
    return (
      <OrderExperience
        view="orders"
        flow={orderFlow}
        catalog={catalogFlow}
        currentUser={currentUser!}
        busy={uiBusy}
        onInvoice={(code) => void documentReportFlow.createInvoiceForOrder(code)}
      />
    );
  }

  function renderPurchases() {
    return (
      <PurchaseExperience
        flow={purchaseFlow}
        products={productLookup}
        canManage={canManagePurchases}
        busy={uiBusy}
      />
    );
  }

  function renderDocuments() {
    return (
      <DocumentReportExperience
        view="documents"
        flow={documentReportFlow}
        busy={uiBusy}
      />
    );
  }

  function renderReports() {
    return (
      <DocumentReportExperience
        view="reports"
        flow={documentReportFlow}
        busy={uiBusy}
      />
    );
  }

  function renderAdministration(view: 'accounts' | 'company' | 'monitoring' | 'audit') {
    return <AdministrationExperience view={view} flow={administrationFlow} isSuperAdmin={Boolean(isSuperAdmin)} busy={uiBusy} />;
  }
}
