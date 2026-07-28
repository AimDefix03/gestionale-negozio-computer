export type ProductCategory = 'HARDWARE' | 'SOFTWARE';
export type BusinessPartnerType = 'CUSTOMER' | 'SUPPLIER';

export type Product = {
  id: number;
  code: string;
  name: string;
  description: string;
  category: ProductCategory;
  brand: string;
  productType: string;
  usageContext: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  price: number;
  discount: number;
  discountedPrice: number;
  discontinued: boolean;
};

export type ProductLookup = {
  code: string;
  name: string;
  brand: string;
  productType: string;
  discontinued: boolean;
  availableQuantity: number;
};

export type ProductPayload = {
  code: string;
  name: string;
  description: string;
  category: ProductCategory;
  brand: string;
  productType: string;
  usageContext: string;
  quantity: number;
  price: number;
  discount: number;
};

export type BusinessPartner = {
  id: number;
  code: string;
  type: BusinessPartnerType;
  typeLabel: string;
  displayName: string;
  taxCode: string;
  vatNumber: string;
  email: string;
  phone: string;
  address: string;
  city: string;
  notes: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type BusinessPartnerPayload = {
  code: string;
  type: BusinessPartnerType;
  displayName: string;
  taxCode: string;
  vatNumber: string;
  email: string;
  phone: string;
  address: string;
  city: string;
  notes: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type PageQuery = {
  page?: number;
  size?: number;
  q?: string;
};

export type ProductQuery = PageQuery & {
  category?: ProductCategory | 'ALL';
  brand?: string;
  productType?: string;
  stock?: 'ALL' | 'AVAILABLE' | 'LOW' | 'OUT';
  sort?: 'NAME_ASC' | 'PRICE_ASC' | 'PRICE_DESC' | 'QTY_ASC' | 'QTY_DESC';
};

export type PartnerQuery = PageQuery & {
  type?: BusinessPartnerType | 'ALL';
  active?: boolean;
};

export type MovementQuery = PageQuery & {
  type?: 'LOAD' | 'UNLOAD' | 'RETURN' | 'ALL';
  productCode?: string;
};

export type OrderQuery = PageQuery & {
  customer?: string;
};

export type AuditQuery = PageQuery & {
  category?: string;
  severity?: 'INFO' | 'WARNING' | 'CRITICAL' | 'ALL';
};

export type DocumentQuery = PageQuery & {
  type?: 'SIMULATED_INVOICE' | 'SIMULATED_CREDIT_NOTE' | 'ALL';
};

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'EMPLOYEE' | 'CUSTOMER';

export type AccountQuery = PageQuery & {
  role?: UserRole | 'ALL';
};

export type UserPermission =
  | 'VIEW_CATALOG'
  | 'VIEW_PARTNERS'
  | 'MANAGE_PARTNERS'
  | 'MANAGE_PRODUCTS'
  | 'MANAGE_INVENTORY'
  | 'VIEW_ORDERS'
  | 'CREATE_ORDERS'
  | 'CONFIRM_ORDERS'
  | 'FULFILL_ORDERS'
  | 'CANCEL_ORDERS'
  | 'RECORD_PAYMENTS'
  | 'REFUND_PAYMENTS'
  | 'REQUEST_RETURNS'
  | 'MANAGE_RETURNS'
  | 'MANAGE_DOCUMENTS'
  | 'VIEW_REPORTS'
  | 'MANAGE_ACCOUNTS'
  | 'MANAGE_COMPANY_SETTINGS'
  | 'VIEW_AUDIT';

export type UserAccount = {
  id: number;
  username: string;
  role: UserRole;
  roleLabel: string;
  permissions: UserPermission[];
};

export type AuthSession = {
  user: UserAccount;
  token: string;
  expiresAt: string;
};

export type PasswordStrength = {
  strength: 'WEAK' | 'MEDIUM' | 'STRONG';
  label: string;
  suggestions: string[];
};

export type StockMovement = {
  id: number;
  timestamp: string;
  actor: string;
  role: string;
  productCode: string;
  productName: string;
  type: 'LOAD' | 'UNLOAD' | 'RETURN';
  typeLabel: string;
  quantity: number;
  previousQuantity: number;
  newQuantity: number;
  reason: string;
};

export type StockMovementPayload = {
  productCode: string;
  type: 'LOAD' | 'UNLOAD';
  quantity: number;
  reason: string;
};

export type Order = {
  id: number;
  code: string;
  customerCode?: string;
  customer: string;
  timestamp: string;
  paymentMethod: string;
  payment: OrderPayment;
  items: OrderItem[];
  returns: OrderReturn[];
  total: number;
  status: 'DRAFT' | 'CONFIRMED' | 'FULFILLED' | 'CANCELED';
  statusLabel: string;
  statusChangedAt: string;
};

export type PaymentMethod = 'CARD' | 'BANK_TRANSFER' | 'CASH';

export type PaymentStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'FAILED' | 'CANCELED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';

export type OrderPayment = {
  id: number;
  method: PaymentMethod | 'OTHER';
  methodLabel: string;
  methodDetails: string;
  status: PaymentStatus;
  statusLabel: string;
  requestedAmount: number;
  paidAmount: number;
  refundedAmount: number;
  netPaidAmount: number;
  outstandingAmount: number;
  refundableAmount: number;
  currency: string;
  createdAt: string;
  updatedAt: string;
  transactions: PaymentTransaction[];
};

export type PaymentTransaction = {
  id: number;
  code: string;
  type: 'RECEIPT' | 'REFUND';
  typeLabel: string;
  amount: number;
  reference: string;
  reason: string;
  returnCode: string;
  recordedAt: string;
  recordedBy: string;
  recordedByRole: string;
};

export type OrderReturnStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'RECEIVED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';

export type OrderReturn = {
  code: string;
  status: OrderReturnStatus;
  statusLabel: string;
  reason: string;
  items: OrderReturnItem[];
  totalAmount: number;
  refundedAmount: number;
  refundableAmount: number;
  requestedAt: string;
  requestedBy: string;
  requestedByRole: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewNote: string;
  receivedAt?: string;
  receivedBy?: string;
  updatedAt: string;
};

export type OrderReturnItem = {
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type ReceiptPayload = { amount: number; reference?: string; reason: string };
export type ReturnRequestPayload = { reason: string; items: { productCode: string; quantity: number }[] };
export type ReturnRefundPayload = { amount: number; reference?: string; reason: string };

export type OrderItem = {
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type CreateOrderPayload = {
  customer: string;
  customerCode?: string;
  paymentMethod: PaymentMethod;
  items: { productCode: string; quantity: number }[];
};

export type FiscalDocument = {
  id: number;
  code: string;
  fiscalYear: number;
  sequenceNumber: number;
  documentPrefix: string;
  type: 'SIMULATED_INVOICE' | 'SIMULATED_CREDIT_NOTE';
  typeLabel: string;
  status: string;
  statusLabel: string;
  createdAt: string;
  relatedOrderCode: string;
  customer: string;
  companySnapshotLegalName: string;
  companySnapshotTaxCode: string;
  companySnapshotVatNumber: string;
  companySnapshotEmail: string;
  companySnapshotPhone: string;
  companySnapshotAddress: string;
  companySnapshotPostalCode: string;
  companySnapshotCity: string;
  companySnapshotProvince: string;
  companySnapshotCountryCode: string;
  customerSnapshotCode: string;
  customerSnapshotName: string;
  customerSnapshotTaxCode: string;
  customerSnapshotVatNumber: string;
  customerSnapshotEmail: string;
  customerSnapshotPhone: string;
  customerSnapshotAddress: string;
  customerSnapshotCity: string;
  paymentMethod: string;
  taxableAmount: number;
  vatRate: number;
  vatAmount: number;
  totalAmount: number;
  createdBy: string;
  createdByRole: string;
  reason: string;
  disclaimer: string;
};

export type CompanySettings = {
  version: number;
  configured: boolean;
  legalName: string;
  taxCode: string;
  vatNumber: string;
  email: string;
  phone: string;
  address: string;
  postalCode: string;
  city: string;
  province: string;
  countryCode: string;
  defaultVatRate: number;
  invoicePrefix: string;
  creditNotePrefix: string;
  numberPadding: number;
  updatedAt: string;
  updatedBy: string;
};

export type CompanySettingsPayload = Omit<CompanySettings, 'configured' | 'updatedAt' | 'updatedBy'>;

export type AuditEvent = {
  id: number;
  timestamp: string;
  actor: string;
  role: string;
  action: string;
  target: string;
  details: string;
  requestId: string;
  source: string;
  entityType: string;
  category: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
};

export type SystemStatus = {
  status: 'UP' | 'DEGRADED' | 'DOWN';
  timestamp: string;
  application: string;
  activeProfile: string;
  uptimeMs: number;
  database: {
    status: 'UP' | 'DOWN';
    latencyMs: number;
    name: string;
    message: string;
  };
  runtime: {
    usedMemoryBytes: number;
    maxMemoryBytes: number;
    availableProcessors: number;
  };
  security: {
    activeSessions: number;
    lockedLoginAttempts: number;
    recentLoginAttempts: number;
  };
  audit: {
    criticalLast24h: number;
    warningsLast24h: number;
    recentImportantEvents: AuditEvent[];
  };
  recentErrors: {
    timestamp: string;
    status: number;
    code: string;
    message: string;
    path: string;
    requestId: string;
  }[];
};

export type DashboardSummary = {
  products: number;
  inventoryValue: number;
  lowStock: number;
  outOfStock: number;
  orders: number;
  revenue: number;
  recentOrders: Order[];
  recentMovements: StockMovement[];
};

export type ReportFormat = 'CSV' | 'XLSX' | 'PDF';

export type SalesReportQuery = {
  from?: string;
  to?: string;
  status?: Order['status'] | 'ALL';
};

export type InventoryReportQuery = {
  q?: string;
  category?: ProductCategory | 'ALL';
  stock?: 'ALL' | 'AVAILABLE' | 'LOW' | 'OUT';
  discontinued?: boolean;
};

export type SalesReport = {
  generatedAt: string;
  from: string;
  to: string;
  status: Order['status'] | 'ALL';
  statusLabel: string;
  orderCount: number;
  orderValue: number;
  paidAmount: number;
  refundedAmount: number;
  netCollectedAmount: number;
  outstandingAmount: number;
  averageOrderValue: number;
  orders: {
    code: string;
    timestamp: string;
    customer: string;
    status: Order['status'];
    statusLabel: string;
    total: number;
    paidAmount: number;
    refundedAmount: number;
    netCollectedAmount: number;
    outstandingAmount: number;
  }[];
  topProducts: {
    productCode: string;
    productName: string;
    quantity: number;
    orderValue: number;
  }[];
};

export type InventoryReport = {
  generatedAt: string;
  productCount: number;
  physicalUnits: number;
  reservedUnits: number;
  availableUnits: number;
  inventoryValue: number;
  lowStockCount: number;
  outOfStockCount: number;
  discontinuedCount: number;
  products: {
    code: string;
    name: string;
    category: ProductCategory;
    categoryLabel: string;
    brand: string;
    productType: string;
    quantity: number;
    reservedQuantity: number;
    availableQuantity: number;
    price: number;
    discount: number;
    discountedPrice: number;
    stockValue: number;
    discontinued: boolean;
    stockStatus: 'AVAILABLE' | 'LOW' | 'OUT';
    stockStatusLabel: string;
  }[];
};
