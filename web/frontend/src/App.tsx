import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  AccountQuery,
  ApiRequestError,
  AuditEvent,
  BusinessPartner,
  BusinessPartnerPayload,
  clearSessionToken,
  CompanySettings,
  CompanySettingsPayload,
  cancelOrder,
  confirmOrder,
  createAccount,
  createCreditNote,
  createInvoice,
  createMovement,
  createOrder,
  createPartner,
  createProduct,
  deactivatePartner,
  deleteAccount,
  deleteProduct,
  deleteProducts,
  discontinueProduct,
  DashboardSummary,
  downloadInventoryReport,
  downloadSalesReport,
  DocumentQuery,
  fetchAccountPage,
  fetchAuditEventPage,
  fetchCompanySettings,
  fetchDashboard,
  fetchDocumentPage,
  fetchMovementPage,
  fetchOrderPage,
  fetchPartnerPage,
  fetchProductPage,
  fetchProductLookup,
  fetchInventoryReport,
  fetchSalesReport,
  fetchSystemStatus,
  fulfillOrder,
  FiscalDocument,
  InventoryReport,
  InventoryReportQuery,
  login,
  logoutSession,
  AuditQuery,
  MovementQuery,
  Order,
  OrderQuery,
  PageResponse,
  PartnerQuery,
  PaymentMethod,
  Product,
  ProductLookup,
  ProductPayload,
  ProductQuery,
  register,
  renewSession,
  recordOrderReceipt,
  requestOrderReturn,
  approveOrderReturn,
  rejectOrderReturn,
  receiveOrderReturn,
  refundOrderReturn,
  ReceiptPayload,
  ReportFormat,
  ReturnRefundPayload,
  ReturnRequestPayload,
  setSessionToken,
  SalesReport,
  SalesReportQuery,
  SessionExpiredError,
  StockMovement,
  SystemStatus,
  updateProduct,
  updatePartner,
  updateCompanySettings,
  UserAccount,
  UserPermission
} from './api';
import SessionRenewalModal from './components/auth/SessionRenewalModal';
import WorkspaceChrome from './components/layout/WorkspaceChrome';
import useWorkspaceNavigation from './hooks/useWorkspaceNavigation';
import AccountsPage from './pages/AccountsPage';
import AuditPage from './pages/AuditPage';
import AuthPage from './pages/AuthPage';
import CatalogPage from './pages/CatalogPage';
import CompanySettingsPage from './pages/CompanySettingsPage';
import DashboardPage from './pages/DashboardPage';
import DocumentsPage from './pages/DocumentsPage';
import InventoryPage from './pages/InventoryPage';
import MonitoringPage from './pages/MonitoringPage';
import OrdersPage from './pages/OrdersPage';
import PartnersPage from './pages/PartnersPage';
import ReportsPage from './pages/ReportsPage';
import { AccountFormState, AuthFormState, AuthMode, CartItem, MenuGroup, MenuItem, MovementFormState, View } from './types/ui';

const DEFAULT_PAGE_SIZE = 8;
const emptyPartnerForm: BusinessPartnerPayload = { code: '', type: 'CUSTOMER', displayName: '', taxCode: '', vatNumber: '', email: '', phone: '', address: '', city: '', notes: '' };

function emptyPage<T>(size = DEFAULT_PAGE_SIZE): PageResponse<T> {
  return { content: [], page: 0, size, totalElements: 0, totalPages: 0, first: true, last: true };
}

function errorMessage(exception: unknown, fallback = 'Operazione non riuscita.'): string {
  if (exception instanceof ApiRequestError || exception instanceof SessionExpiredError) {
    return exception.requestId ? `${exception.message} Codice richiesta: ${exception.requestId}` : exception.message;
  }
  return exception instanceof Error ? exception.message : fallback;
}

export default function App() {
  const [currentUser, setCurrentUser] = useState<UserAccount | null>(null);
  const [sessionExpiresAt, setSessionExpiresAt] = useState('');
  const { activeView, openTabs, openMenu, setActiveView, setOpenMenu, openTab, closeTab, resetNavigation } = useWorkspaceNavigation();
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [authForm, setAuthForm] = useState<AuthFormState>({ username: '', password: '', role: 'SUPER_ADMIN' });
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [productLookup, setProductLookup] = useState<ProductLookup[]>([]);
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null);
  const [productQuery, setProductQuery] = useState<ProductQuery>({ page: 0, size: DEFAULT_PAGE_SIZE, sort: 'NAME_ASC' });
  const [partnerQuery, setPartnerQuery] = useState<PartnerQuery>({ page: 0, size: DEFAULT_PAGE_SIZE, active: true });
  const [movementQuery, setMovementQuery] = useState<MovementQuery>({ page: 0, size: DEFAULT_PAGE_SIZE });
  const [orderQuery, setOrderQuery] = useState<OrderQuery>({ page: 0, size: DEFAULT_PAGE_SIZE });
  const [documentQuery, setDocumentQuery] = useState<DocumentQuery>({ page: 0, size: DEFAULT_PAGE_SIZE });
  const [accountQuery, setAccountQuery] = useState<AccountQuery>({ page: 0, size: DEFAULT_PAGE_SIZE });
  const [auditQuery, setAuditQuery] = useState<AuditQuery>({ page: 0, size: DEFAULT_PAGE_SIZE });
  const [salesReportQuery, setSalesReportQuery] = useState<SalesReportQuery>({ status: 'FULFILLED' });
  const [inventoryReportQuery, setInventoryReportQuery] = useState<InventoryReportQuery>({ stock: 'ALL', discontinued: false });
  const [productPage, setProductPage] = useState<PageResponse<Product>>(emptyPage<Product>());
  const [partnerPage, setPartnerPage] = useState<PageResponse<BusinessPartner>>(emptyPage<BusinessPartner>());
  const [movementPage, setMovementPage] = useState<PageResponse<StockMovement>>(emptyPage<StockMovement>());
  const [orderPage, setOrderPage] = useState<PageResponse<Order>>(emptyPage<Order>());
  const [documentPage, setDocumentPage] = useState<PageResponse<FiscalDocument>>(emptyPage<FiscalDocument>());
  const [accountPage, setAccountPage] = useState<PageResponse<UserAccount>>(emptyPage<UserAccount>());
  const [auditPage, setAuditPage] = useState<PageResponse<AuditEvent>>(emptyPage<AuditEvent>());
  const [companySettings, setCompanySettings] = useState<CompanySettings | null>(null);
  const [salesReport, setSalesReport] = useState<SalesReport | null>(null);
  const [inventoryReport, setInventoryReport] = useState<InventoryReport | null>(null);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [focusedProductCode, setFocusedProductCode] = useState('');
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [editingPartner, setEditingPartner] = useState<BusinessPartner | null>(null);
  const [partnerForm, setPartnerForm] = useState<BusinessPartnerPayload>(emptyPartnerForm);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD');
  const [movementForm, setMovementForm] = useState<MovementFormState>({ productCode: '', type: 'LOAD', quantity: '1', reason: '' });
  const [accountForm, setAccountForm] = useState<AccountFormState>({ username: '', password: '', role: 'EMPLOYEE' });
  const [reauthPassword, setReauthPassword] = useState('');
  const [sessionPassword, setSessionPassword] = useState('');
  const [sessionRenewalOpen, setSessionRenewalOpen] = useState(false);
  const [sessionRenewalMessage, setSessionRenewalMessage] = useState('');
  const [sessionRenewalBusy, setSessionRenewalBusy] = useState(false);
  const [creditReason, setCreditReason] = useState('Rettifica documento simulato');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN';
  const hasPermission = (permission: UserPermission) => Boolean(currentUser?.permissions.includes(permission));
  const canManageProducts = hasPermission('MANAGE_PRODUCTS');
  const canViewPartners = hasPermission('VIEW_PARTNERS');
  const canManagePartners = hasPermission('MANAGE_PARTNERS');
  const canManageInventory = hasPermission('MANAGE_INVENTORY');
  const canManageDocuments = hasPermission('MANAGE_DOCUMENTS');
  const canRecordPayments = hasPermission('RECORD_PAYMENTS');
  const canRefundPayments = hasPermission('REFUND_PAYMENTS');
  const canRequestReturns = hasPermission('REQUEST_RETURNS');
  const canManageReturns = hasPermission('MANAGE_RETURNS');
  const canManageAccounts = hasPermission('MANAGE_ACCOUNTS');
  const canManageCompanySettings = hasPermission('MANAGE_COMPANY_SETTINGS');
  const canViewAudit = hasPermission('VIEW_AUDIT');
  const canViewReports = hasPermission('VIEW_REPORTS');
  const visibleOrders = orderPage.content;
  const focusedProduct = useMemo(() => productPage.content.find((product) => product.code === focusedProductCode) ?? productPage.content[0] ?? null, [focusedProductCode, productPage.content]);
  const focusedProductMovements = useMemo(() => focusedProduct ? movementPage.content.filter((movement) => movement.productCode === focusedProduct.code) : [], [focusedProduct, movementPage.content]);
  const focusedProductOrders = useMemo(() => focusedProduct ? visibleOrders.filter((order) => order.items.some((item) => item.productCode === focusedProduct.code)) : [], [focusedProduct, visibleOrders]);

  const stats = useMemo(() => {
    if (dashboard) {
      return {
        products: dashboard.products,
        inventoryValue: dashboard.inventoryValue,
        lowStock: dashboard.lowStock,
        outOfStock: dashboard.outOfStock,
        orders: dashboard.orders,
        revenue: dashboard.revenue
      };
    }
    const inventoryValue = productPage.content.reduce((total, product) => total + product.discountedPrice * product.quantity, 0);
    return {
      products: productPage.totalElements,
      inventoryValue,
      lowStock: productPage.content.filter((product) => product.availableQuantity > 0 && product.availableQuantity <= 3).length,
      outOfStock: productPage.content.filter((product) => product.availableQuantity === 0).length,
      orders: visibleOrders.length,
      revenue: visibleOrders.reduce((total, order) => total + order.total, 0)
    };
  }, [dashboard, productPage.content, productPage.totalElements, visibleOrders]);

  useEffect(() => {
    if (currentUser) {
      void loadWorkspace();
    }
  }, [currentUser]);

  useEffect(() => {
    if (currentUser) void loadProductPage();
  }, [currentUser, productQuery]);

  useEffect(() => {
    if (currentUser && canViewPartners) void loadPartnerPage();
  }, [currentUser, canViewPartners, partnerQuery]);

  useEffect(() => {
    if (currentUser && canManageInventory) void loadMovementPage();
  }, [currentUser, canManageInventory, movementQuery]);

  useEffect(() => {
    if (currentUser) void loadOrderPage();
  }, [currentUser, orderQuery]);

  useEffect(() => {
    if (currentUser && canManageDocuments) void loadDocumentPage();
  }, [currentUser, canManageDocuments, documentQuery]);

  useEffect(() => {
    if (currentUser && canManageAccounts) void loadAccountPage();
  }, [currentUser, canManageAccounts, accountQuery]);

  useEffect(() => {
    if (currentUser && canViewAudit) void loadAuditPage();
  }, [currentUser, canViewAudit, auditQuery]);

  useEffect(() => {
    if (currentUser && canViewAudit && activeView === 'monitoring') void loadSystemStatus();
  }, [currentUser, canViewAudit, activeView]);

  useEffect(() => {
    if (currentUser && canViewReports && activeView === 'reports') void loadSalesReport();
  }, [currentUser, canViewReports, activeView, salesReportQuery]);

  useEffect(() => {
    if (currentUser && canViewReports && activeView === 'reports') void loadInventoryReport();
  }, [currentUser, canViewReports, activeView, inventoryReportQuery]);

  useEffect(() => {
    if (!currentUser || !sessionExpiresAt) return;

    const checkSession = () => {
      const remaining = new Date(sessionExpiresAt).getTime() - Date.now();
      if (remaining <= 0) {
        openSessionRenewal('Sessione scaduta. Riconferma la password per continuare.');
        return;
      }
      if (remaining <= 3 * 60 * 1000) {
        openSessionRenewal('La sessione sta per scadere. Rinnovala prima di continuare con operazioni delicate.');
      }
    };

    checkSession();
    const interval = window.setInterval(checkSession, 30000);
    return () => window.clearInterval(interval);
  }, [currentUser, sessionExpiresAt]);

  useEffect(() => {
    if (productPage.content.length === 0) {
      if (focusedProductCode) setFocusedProductCode('');
      return;
    }

    if (!focusedProductCode || !productPage.content.some((product) => product.code === focusedProductCode)) {
      setFocusedProductCode(productPage.content[0].code);
    }
  }, [focusedProductCode, productPage.content]);

  async function loadWorkspace() {
    setMessage('');
    await Promise.all([loadWorkspaceSnapshot(), loadPagedViews()]);
  }

  async function loadWorkspaceSnapshot() {
    const [dashboardData, lookupData] = await Promise.all([
      fetchDashboard(),
      fetchProductLookup()
    ]);
    setDashboard(dashboardData);
    setProductLookup(lookupData);
  }

  async function loadPagedViews() {
    await Promise.all([
      loadProductPage(),
      canViewPartners ? loadPartnerPage() : Promise.resolve(setPartnerPage(emptyPage<BusinessPartner>(partnerQuery.size))),
      loadOrderPage(),
      canManageDocuments ? loadDocumentPage() : Promise.resolve(setDocumentPage(emptyPage<FiscalDocument>(documentQuery.size))),
      canManageAccounts ? loadAccountPage() : Promise.resolve(setAccountPage(emptyPage<UserAccount>(accountQuery.size))),
      canManageInventory ? loadMovementPage() : Promise.resolve(setMovementPage(emptyPage<StockMovement>(movementQuery.size))),
      canViewAudit ? loadAuditPage() : Promise.resolve(setAuditPage(emptyPage<AuditEvent>(auditQuery.size))),
      canManageCompanySettings ? loadCompanySettings() : Promise.resolve(setCompanySettings(null))
    ]);
  }

  async function loadProductPage() {
    setProductPage(await fetchProductPage(productQuery));
  }

  async function loadPartnerPage() {
    setPartnerPage(await fetchPartnerPage(partnerQuery));
  }

  async function loadMovementPage() {
    setMovementPage(await fetchMovementPage(movementQuery));
  }

  async function loadOrderPage() {
    setOrderPage(await fetchOrderPage(orderQuery));
  }

  async function loadDocumentPage() {
    setDocumentPage(await fetchDocumentPage(documentQuery));
  }

  async function loadAccountPage() {
    setAccountPage(await fetchAccountPage(accountQuery));
  }

  async function loadAuditPage() {
    setAuditPage(await fetchAuditEventPage(auditQuery));
  }

  async function loadSystemStatus() {
    setSystemStatus(await fetchSystemStatus());
  }

  async function loadCompanySettings() {
    setCompanySettings(await fetchCompanySettings());
  }

  async function loadSalesReport() {
    setSalesReport(await fetchSalesReport(salesReportQuery));
  }

  async function loadInventoryReport() {
    setInventoryReport(await fetchInventoryReport(inventoryReportQuery));
  }

  async function handleReportExport(kind: 'sales' | 'inventory', format: ReportFormat) {
    await run(async () => {
      if (kind === 'sales') await downloadSalesReport(salesReportQuery, format);
      else await downloadInventoryReport(inventoryReportQuery, format);
      setMessage(`Report ${kind === 'sales' ? 'vendite' : 'magazzino'} esportato in formato ${format}.`);
    });
  }

  async function handleAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await run(async () => {
      const role = authMode === 'register' && authForm.role !== 'CUSTOMER' ? 'EMPLOYEE' : authForm.role;
      if (authMode === 'register') {
        await register(authForm.username, authForm.password, role === 'CUSTOMER' ? 'CUSTOMER' : 'EMPLOYEE');
      }
      const session = await login(authForm.username, authForm.password, role);
      setSessionToken(session.token);
      setSessionExpiresAt(session.expiresAt);
      setCurrentUser(session.user);
      resetNavigation();
    });
  }

  async function handleSessionRenewal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentUser) return;
    setSessionRenewalBusy(true);
    setSessionRenewalMessage('');
    try {
      let session;
      try {
        session = await renewSession(sessionPassword);
      } catch (exception) {
        if (!(exception instanceof SessionExpiredError)) throw exception;
        session = await login(currentUser.username, sessionPassword, currentUser.role);
      }
      setSessionToken(session.token);
      setSessionExpiresAt(session.expiresAt);
      setCurrentUser(session.user);
      setSessionPassword('');
      setSessionRenewalOpen(false);
      setMessage('Sessione rinnovata. Puoi continuare.');
    } catch (exception) {
      setSessionRenewalMessage(errorMessage(exception, 'Impossibile rinnovare la sessione.'));
    } finally {
      setSessionRenewalBusy(false);
    }
  }

  async function handleProductSubmit(payload: ProductPayload) {
    await run(async () => {
      if (editingProduct) {
        await updateProduct(editingProduct.code, payload);
      } else {
        await createProduct(payload);
      }
      setEditingProduct(null);
      await loadWorkspace();
    });
  }

  async function handlePartnerSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await run(async () => {
      if (editingPartner) {
        await updatePartner(editingPartner.code, partnerForm);
      } else {
        await createPartner(partnerForm);
      }
      setEditingPartner(null);
      setPartnerForm(emptyPartnerForm);
      await loadWorkspace();
    });
  }

  async function handleDeactivatePartner(code: string) {
    if (!window.confirm(`Disattivare l'anagrafica ${code}?`)) return;
    await run(async () => {
      await deactivatePartner(code);
      if (editingPartner?.code === code) {
        setEditingPartner(null);
        setPartnerForm(emptyPartnerForm);
      }
      await loadWorkspace();
    });
  }

  function startEditPartner(partner: BusinessPartner) {
    setEditingPartner(partner);
    setPartnerForm(partnerToForm(partner));
  }

  async function handleDeleteProduct(code: string) {
    if (!window.confirm(`Eliminare il prodotto ${code}?`)) return;
    await run(async () => {
      await deleteProduct(code);
      await loadWorkspace();
    });
  }

  async function handleDeleteSelectedProducts() {
    if (selectedCodes.length === 0 || !window.confirm(`Eliminare ${selectedCodes.length} prodotti?`)) return;
    await run(async () => {
      await deleteProducts(selectedCodes);
      setSelectedCodes([]);
      await loadWorkspace();
    });
  }

  async function handleDiscontinueProduct(code: string) {
    if (!window.confirm(`Disattivare il prodotto ${code}? Non sara disponibile per nuovi ordini.`)) return;
    await run(async () => {
      await discontinueProduct(code);
      await loadWorkspace();
    });
  }

  async function handleMovement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentUser) return;
    await run(async () => {
      await createMovement({
        productCode: movementForm.productCode,
        type: movementForm.type,
        quantity: Number(movementForm.quantity),
        reason: movementForm.reason
      });
      setMovementForm({ productCode: '', type: 'LOAD', quantity: '1', reason: '' });
      await loadWorkspace();
    });
  }

  async function handleCheckout() {
    if (!currentUser || cart.length === 0) return;
    await run(async () => {
      await createOrder({
        customer: currentUser.username,
        paymentMethod,
        items: cart.map((item) => ({ productCode: item.product.code, quantity: item.quantity }))
      });
      setCart([]);
      setPaymentMethod('CARD');
      await loadWorkspace();
      openTab('orders');
      setMessage('Ordine creato in bozza. Confermalo dalla sezione Ordini per scaricare il magazzino.');
    });
  }

  async function handleConfirmOrder(orderCode: string) {
    await run(async () => {
      await confirmOrder(orderCode);
      await loadWorkspace();
    });
  }

  async function handleFulfillOrder(orderCode: string) {
    await run(async () => {
      await fulfillOrder(orderCode);
      await loadWorkspace();
    });
  }

  async function handleCancelOrder(orderCode: string) {
    if (!window.confirm(`Annullare l'ordine ${orderCode}?`)) return;
    await run(async () => {
      await cancelOrder(orderCode);
      await loadWorkspace();
    });
  }

  async function handleOrderReceipt(orderCode: string, payload: ReceiptPayload) {
    await run(async () => {
      await recordOrderReceipt(orderCode, payload);
      await loadWorkspace();
      setMessage(`Incasso registrato sull'ordine ${orderCode}.`);
    });
  }

  async function handleRequestReturn(orderCode: string, payload: ReturnRequestPayload) {
    await run(async () => {
      await requestOrderReturn(orderCode, payload);
      await loadWorkspace();
      setMessage(`Richiesta di reso registrata per l'ordine ${orderCode}.`);
    });
  }

  async function handleApproveReturn(orderCode: string, returnCode: string, note: string) {
    await run(async () => {
      await approveOrderReturn(orderCode, returnCode, note);
      await loadWorkspace();
    });
  }

  async function handleRejectReturn(orderCode: string, returnCode: string, note: string) {
    if (!note.trim()) {
      setMessage('Inserisci una motivazione prima di rifiutare il reso.');
      return;
    }
    await run(async () => {
      await rejectOrderReturn(orderCode, returnCode, note);
      await loadWorkspace();
    });
  }

  async function handleReceiveReturn(orderCode: string, returnCode: string) {
    await run(async () => {
      await receiveOrderReturn(orderCode, returnCode);
      await loadWorkspace();
      setMessage(`Reso ${returnCode} ricevuto e magazzino aggiornato.`);
    });
  }

  async function handleRefundReturn(orderCode: string, returnCode: string, payload: ReturnRefundPayload) {
    await run(async () => {
      await refundOrderReturn(orderCode, returnCode, payload);
      await loadWorkspace();
      setMessage(`Rimborso registrato sul reso ${returnCode}.`);
    });
  }

  async function handleCreateAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentUser) return;
    if (!reauthPassword) {
      setMessage('Inserisci la password della sessione per gestire gli account.');
      return;
    }
    await run(async () => {
      await createAccount(accountForm.username, accountForm.password, accountForm.role, reauthPassword);
      setAccountForm({ username: '', password: '', role: 'EMPLOYEE' });
      setReauthPassword('');
      await loadWorkspace();
    });
  }

  async function handleCompanySettingsSave(payload: CompanySettingsPayload) {
    await run(async () => {
      const updated = await updateCompanySettings(payload);
      setCompanySettings(updated);
      setMessage('Configurazione aziendale aggiornata. I nuovi documenti useranno questi dati.');
    });
  }

  async function handleDeleteAccount(username: string) {
    if (!currentUser || !window.confirm(`Eliminare l'account ${username}?`)) return;
    if (!reauthPassword) {
      setMessage('Inserisci la password della sessione per eliminare account.');
      return;
    }
    await run(async () => {
      await deleteAccount(username, reauthPassword);
      setReauthPassword('');
      await loadWorkspace();
    });
  }

  async function handleInvoice(orderCode: string) {
    if (!currentUser) return;
    await run(async () => {
      await createInvoice(orderCode);
      await loadWorkspace();
      openTab('documents');
    });
  }

  async function handleCreditNote(orderCode: string) {
    if (!currentUser) return;
    await run(async () => {
      await createCreditNote(orderCode, creditReason);
      await loadWorkspace();
      openTab('documents');
    });
  }

  function addToCart(product: Product) {
    if (product.discontinued) {
      setMessage('Prodotto disattivato e non disponibile per nuovi ordini.');
      return;
    }
    if (product.availableQuantity <= 0) {
      setMessage('Prodotto non disponibile alla vendita.');
      return;
    }
    setCart((current) => {
      const existing = current.find((item) => item.product.code === product.code);
      if (existing) {
        if (existing.quantity >= product.availableQuantity) {
          setMessage('Disponibilita vendibile massima gia selezionata.');
          return current;
        }
        return current.map((item) => item.product.code === product.code ? { ...item, quantity: item.quantity + 1 } : item);
      }
      return [...current, { product, quantity: 1 }];
    });
  }

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setMessage('');
    try {
      await action();
    } catch (exception) {
      if (exception instanceof SessionExpiredError && currentUser) {
        openSessionRenewal(errorMessage(exception));
        return;
      }
      setMessage(errorMessage(exception));
    } finally {
      setBusy(false);
    }
  }

  function openSessionRenewal(message: string) {
    setSessionRenewalMessage(message);
    setSessionRenewalOpen(true);
  }

  if (!currentUser) {
    return (
      <AuthPage
        mode={authMode}
        form={authForm}
        passwordVisible={passwordVisible}
        busy={busy}
        message={message}
        onModeChange={setAuthMode}
        onFormChange={setAuthForm}
        onPasswordVisibilityChange={setPasswordVisible}
        onSubmit={handleAuth}
      />
    );
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
        canManageInventory ? { label: 'Magazzino', description: 'Carichi, scarichi e movimenti', view: 'inventory' as View } : null,
        { label: 'Ordini', description: 'Acquisti e storico transazioni', view: 'orders' as View },
        canManageDocuments ? { label: 'Documenti', description: 'Fatture e note credito simulate', view: 'documents' as View } : null,
        canViewReports ? { label: 'Report', description: 'Vendite, incassi e situazione magazzino', view: 'reports' as View } : null
      ].filter(Boolean) as MenuItem[]
    },
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
        { label: 'Aggiorna dati', description: 'Ricarica workspace e dati recenti', action: () => void loadWorkspace() },
        { label: 'Logout', description: 'Chiude la sessione corrente', action: handleLogout }
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
        activeView={activeView}
        message={message}
        onMenuChange={setOpenMenu}
        onOpenTab={openTab}
        onCloseTab={closeTab}
        onActiveViewChange={setActiveView}
        onRefresh={() => void loadWorkspace()}
        onLogout={() => void handleLogout()}
      >
        {renderActiveView()}
      </WorkspaceChrome>
      {sessionRenewalOpen && (
        <SessionRenewalModal
          message={sessionRenewalMessage}
          password={sessionPassword}
          busy={sessionRenewalBusy}
          onPasswordChange={setSessionPassword}
          onSubmit={handleSessionRenewal}
          onLogout={() => void handleLogout()}
        />
      )}
    </>
  );

  async function handleLogout() {
    await logoutSession().catch(() => undefined);
    clearSessionToken();
    setCurrentUser(null);
    setSessionExpiresAt('');
    setReauthPassword('');
    setSessionPassword('');
    setSessionRenewalOpen(false);
    setSessionRenewalMessage('');
    resetNavigation();
    setProductQuery({ page: 0, size: DEFAULT_PAGE_SIZE, sort: 'NAME_ASC' });
    setPartnerQuery({ page: 0, size: DEFAULT_PAGE_SIZE, active: true });
    setMovementQuery({ page: 0, size: DEFAULT_PAGE_SIZE });
    setOrderQuery({ page: 0, size: DEFAULT_PAGE_SIZE });
    setDocumentQuery({ page: 0, size: DEFAULT_PAGE_SIZE });
    setAccountQuery({ page: 0, size: DEFAULT_PAGE_SIZE });
    setAuditQuery({ page: 0, size: DEFAULT_PAGE_SIZE });
    setSalesReportQuery({ status: 'FULFILLED' });
    setInventoryReportQuery({ stock: 'ALL', discontinued: false });
    setProductPage(emptyPage<Product>());
    setPartnerPage(emptyPage<BusinessPartner>());
    setMovementPage(emptyPage<StockMovement>());
    setOrderPage(emptyPage<Order>());
    setDocumentPage(emptyPage<FiscalDocument>());
    setAccountPage(emptyPage<UserAccount>());
    setAuditPage(emptyPage<AuditEvent>());
    setCompanySettings(null);
    setSalesReport(null);
    setInventoryReport(null);
    setDashboard(null);
    setProductLookup([]);
    setSystemStatus(null);
  }

  function renderActiveView() {
    if (activeView === 'dashboard') return renderDashboard();
    if (activeView === 'catalog') return renderCatalog();
    if (activeView === 'partners') return renderPartners();
    if (activeView === 'inventory') return renderInventory();
    if (activeView === 'orders') return renderOrders();
    if (activeView === 'documents') return renderDocuments();
    if (activeView === 'reports') return renderReports();
    if (activeView === 'accounts') return renderAccounts();
    if (activeView === 'company') return renderCompanySettings();
    if (activeView === 'monitoring') return renderMonitoring();
    if (activeView === 'audit') return renderAudit();
    return renderDashboard();
  }

  function renderDashboard() {
    const recentOrders = dashboard?.recentOrders ?? visibleOrders.slice(0, 4);
    const recentMovements = dashboard?.recentMovements ?? movementPage.content.slice(0, 4);
    return <DashboardPage stats={stats} recentOrders={recentOrders} recentMovements={recentMovements} />;
  }

  function renderCatalog() {
    return (
      <CatalogPage
        productPage={productPage}
        productLookup={productLookup}
        productQuery={productQuery}
        selectedCodes={selectedCodes}
        focusedProduct={focusedProduct}
        focusedProductMovements={focusedProductMovements}
        focusedProductOrders={focusedProductOrders}
        editingProduct={editingProduct}
        cart={cart}
        paymentMethod={paymentMethod}
        busy={busy}
        canManageProducts={Boolean(canManageProducts)}
        canManageInventory={Boolean(canManageInventory)}
        onQueryChange={(nextQuery) => {
          setSelectedCodes([]);
          setProductQuery(nextQuery);
        }}
        onPageChange={(page) => setProductQuery((current) => ({ ...current, page }))}
        onSelectionChange={setSelectedCodes}
        onFocusProduct={(product) => setFocusedProductCode(product.code)}
        onEditProduct={(product) => {
          setFocusedProductCode(product.code);
          setEditingProduct(product);
        }}
        onDeleteProduct={(code) => void handleDeleteProduct(code)}
        onDiscontinueProduct={(code) => void handleDiscontinueProduct(code)}
        onDeleteSelected={() => void handleDeleteSelectedProducts()}
        onAddToCart={addToCart}
        onMovement={(product) => {
          setMovementForm({ ...movementForm, productCode: product.code });
          openTab('inventory');
        }}
        onProductSubmit={handleProductSubmit}
        onCancelEdit={() => setEditingProduct(null)}
        onCheckout={() => void handleCheckout()}
        onPaymentMethodChange={setPaymentMethod}
        onClearCart={() => setCart([])}
      />
    );
  }

  function renderPartners() {
    return (
      <PartnersPage
        page={partnerPage}
        query={partnerQuery}
        form={partnerForm}
        editingPartner={editingPartner}
        canManage={Boolean(canManagePartners)}
        busy={busy}
        pageSize={DEFAULT_PAGE_SIZE}
        onQueryChange={setPartnerQuery}
        onFormChange={setPartnerForm}
        onEdit={startEditPartner}
        onDeactivate={(code) => void handleDeactivatePartner(code)}
        onCancelEdit={() => {
          setEditingPartner(null);
          setPartnerForm(emptyPartnerForm);
        }}
        onSubmit={handlePartnerSubmit}
      />
    );
  }

  function renderInventory() {
    return (
      <InventoryPage
        page={movementPage}
        query={movementQuery}
        form={movementForm}
        products={productLookup}
        busy={busy}
        pageSize={DEFAULT_PAGE_SIZE}
        onQueryChange={setMovementQuery}
        onFormChange={setMovementForm}
        onSubmit={handleMovement}
      />
    );
  }

  function renderOrders() {
    return (
      <OrdersPage
        page={orderPage}
        query={orderQuery}
        currentUser={currentUser!}
        canManageDocuments={Boolean(canManageDocuments)}
        canRecordPayments={Boolean(canRecordPayments)}
        canRequestReturns={Boolean(canRequestReturns)}
        canManageReturns={Boolean(canManageReturns)}
        canRefundPayments={Boolean(canRefundPayments)}
        busy={busy}
        pageSize={DEFAULT_PAGE_SIZE}
        canConfirm={canConfirmOrder}
        canFulfill={canFulfillOrder}
        canCancel={canCancelOrder}
        onQueryChange={setOrderQuery}
        onConfirm={(code) => void handleConfirmOrder(code)}
        onFulfill={(code) => void handleFulfillOrder(code)}
        onCancel={(code) => void handleCancelOrder(code)}
        onInvoice={(code) => void handleInvoice(code)}
        onReceipt={(code, payload) => void handleOrderReceipt(code, payload)}
        onRequestReturn={(code, payload) => void handleRequestReturn(code, payload)}
        onApproveReturn={(orderCode, returnCode, note) => void handleApproveReturn(orderCode, returnCode, note)}
        onRejectReturn={(orderCode, returnCode, note) => void handleRejectReturn(orderCode, returnCode, note)}
        onReceiveReturn={(orderCode, returnCode) => void handleReceiveReturn(orderCode, returnCode)}
        onRefundReturn={(orderCode, returnCode, payload) => void handleRefundReturn(orderCode, returnCode, payload)}
      />
    );
  }

  function renderDocuments() {
    return (
      <DocumentsPage
        page={documentPage}
        query={documentQuery}
        creditReason={creditReason}
        pageSize={DEFAULT_PAGE_SIZE}
        onQueryChange={setDocumentQuery}
        onCreditReasonChange={setCreditReason}
        onCreditNote={(orderCode) => void handleCreditNote(orderCode)}
      />
    );
  }

  function renderAccounts() {
    return (
      <AccountsPage
        page={accountPage}
        query={accountQuery}
        form={accountForm}
        reauthPassword={reauthPassword}
        isSuperAdmin={Boolean(isSuperAdmin)}
        busy={busy}
        pageSize={DEFAULT_PAGE_SIZE}
        onQueryChange={setAccountQuery}
        onFormChange={setAccountForm}
        onReauthPasswordChange={setReauthPassword}
        onSubmit={handleCreateAccount}
        canDelete={canDeleteAccount}
        protectionLabel={accountProtectionLabel}
        onDelete={(username) => void handleDeleteAccount(username)}
      />
    );
  }

  function renderCompanySettings() {
    return (
      <CompanySettingsPage
        settings={companySettings}
        busy={busy}
        onSave={(payload) => void handleCompanySettingsSave(payload)}
        onReload={() => void run(loadCompanySettings)}
      />
    );
  }

  function renderReports() {
    return (
      <ReportsPage
        sales={salesReport}
        inventory={inventoryReport}
        salesQuery={salesReportQuery}
        inventoryQuery={inventoryReportQuery}
        busy={busy}
        onSalesQueryChange={setSalesReportQuery}
        onInventoryQueryChange={setInventoryReportQuery}
        onExportSales={(format) => void handleReportExport('sales', format)}
        onExportInventory={(format) => void handleReportExport('inventory', format)}
        onRefreshSales={() => void run(loadSalesReport)}
        onRefreshInventory={() => void run(loadInventoryReport)}
      />
    );
  }

  function canDeleteAccount(account: UserAccount) {
    if (!currentUser || !canManageAccounts) return false;
    if (account.username.toLowerCase() === currentUser.username.toLowerCase()) return false;
    if (account.role === 'SUPER_ADMIN') return false;
    if (account.role === 'ADMIN' && !isSuperAdmin) return false;
    return true;
  }

  function accountProtectionLabel(account: UserAccount) {
    if (currentUser && account.username.toLowerCase() === currentUser.username.toLowerCase()) return 'Sessione attiva';
    if (account.role === 'SUPER_ADMIN') return 'Protetto';
    if (account.role === 'ADMIN' && !isSuperAdmin) return 'Solo super admin';
    return 'Non disponibile';
  }

  function canConfirmOrder(order: Order) {
    if (order.status !== 'DRAFT') return false;
    if (!hasPermission('CONFIRM_ORDERS')) return false;
    if (currentUser?.role === 'CUSTOMER') return order.customer.toLowerCase() === currentUser.username.toLowerCase();
    return true;
  }

  function canFulfillOrder(order: Order) {
    return order.status === 'CONFIRMED' && hasPermission('FULFILL_ORDERS');
  }

  function canCancelOrder(order: Order) {
    if (order.status !== 'DRAFT' && order.status !== 'CONFIRMED') return false;
    if (!hasPermission('CANCEL_ORDERS')) return false;
    if (currentUser?.role === 'CUSTOMER') return order.status === 'DRAFT' && order.customer.toLowerCase() === currentUser.username.toLowerCase();
    return true;
  }

  function renderAudit() {
    return (
      <AuditPage
        page={auditPage}
        query={auditQuery}
        pageSize={DEFAULT_PAGE_SIZE}
        onQueryChange={setAuditQuery}
      />
    );
  }

  function renderMonitoring() {
    return <MonitoringPage status={systemStatus} onRefresh={() => void loadSystemStatus()} />;
  }
}

function partnerToForm(partner: BusinessPartner): BusinessPartnerPayload {
  return {
    code: partner.code,
    type: partner.type,
    displayName: partner.displayName,
    taxCode: partner.taxCode,
    vatNumber: partner.vatNumber,
    email: partner.email,
    phone: partner.phone,
    address: partner.address,
    city: partner.city,
    notes: partner.notes
  };
}
