export type ProductCategory = 'HARDWARE' | 'SOFTWARE';
export type BusinessPartnerType = 'CUSTOMER' | 'SUPPLIER';

export type SellableProduct = {
  code: string;
  name: string;
  description: string;
  category: ProductCategory;
  brand: string;
  productType: string;
  usageContext: string;
  price: number;
  discount: number;
  discountedPrice: number;
};

export type Product = SellableProduct & {
  id: number;
  discontinued: boolean;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  lastPurchaseCost: number | null;
  averagePurchaseCost: number | null;
  costedQuantity: number;
  uncostedQuantity: number;
  costCoveragePercentage: number;
  knownInventoryCost: number;
  potentialGrossMarginOnCostedStock: number;
  capabilities: ProductCapabilities;
};

export type ProductCapabilities = {
  canEdit: boolean;
  canChangeCode: boolean;
  canDelete: boolean;
  canDiscontinue: boolean;
  canMoveStock: boolean;
};

export type ProductOrderHistory = {
  code: string;
  customer: string;
  status: Order['status'];
  statusLabel: string;
  timestamp: string;
  quantity: number;
  lineTotal: number;
};

export type ProductOperationalDetail = {
  product: Product;
  recentMovements: StockMovement[];
  recentOrders: ProductOrderHistory[];
};

export type CommercialAvailability = 'AVAILABLE' | 'LIMITED' | 'UNAVAILABLE';

export type CustomerProduct = SellableProduct & {
  availability: CommercialAvailability;
  availabilityLabel: string;
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
  linkedAccountId?: number;
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
  type?: StockMovementType | 'ALL';
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
  enabled?: boolean;
};

export type UserPermission =
  | 'VIEW_CATALOG'
  | 'VIEW_PARTNERS'
  | 'MANAGE_PARTNERS'
  | 'MANAGE_PRODUCTS'
  | 'MANAGE_INVENTORY'
  | 'APPROVE_INVENTORY_COUNTS'
  | 'VIEW_ORDERS'
  | 'VIEW_PURCHASE_ORDERS'
  | 'MANAGE_PURCHASE_ORDERS'
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

export type SupplierOrderStatus = 'DRAFT' | 'SENT' | 'PARTIALLY_RECEIVED' | 'RECEIVED' | 'CANCELED';

export type SupplierOrderQuery = PageQuery & {
  status?: SupplierOrderStatus | 'ALL';
  supplierId?: number;
};

export type SupplierOrderSummary = {
  code: string;
  supplierId: number;
  supplierCode: string;
  supplierName: string;
  status: SupplierOrderStatus;
  statusLabel: string;
  expectedDeliveryDate: string;
  total: number;
  currency: string;
  orderedQuantity: number;
  receivedQuantity: number;
  createdAt: string;
};

export type SupplierOrder = SupplierOrderSummary & {
  id: number;
  notes: string;
  createdBy: string;
  createdByRole: string;
  sentAt: string | null;
  canceledAt: string | null;
  canceledBy: string | null;
  canceledByRole: string | null;
  cancellationReason: string | null;
  items: SupplierOrderItem[];
  receipts: SupplierOrderReceipt[];
  capabilities: SupplierOrderCapabilities;
};

export type SupplierOrderItem = {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  orderedQuantity: number;
  receivedQuantity: number;
  remainingQuantity: number;
  unitPrice: number;
  lineTotal: number;
  expectedDeliveryDate: string;
};

export type SupplierOrderReceipt = {
  code: string;
  reason: string;
  receivedAt: string;
  receivedBy: string;
  receivedByRole: string;
  items: {
    lineId: number;
    productCode: string;
    quantity: number;
    expectedUnitCost: number;
    actualUnitCost: number;
    unitCostVariance: number;
    totalCost: number;
    inventoryPostingStatus: 'PENDING' | 'POSTED' | 'LEGACY_UNPOSTED';
    stockMovementId: number | null;
  }[];
};

export type SupplierOrderCapabilities = {
  canSend: boolean;
  canReceive: boolean;
  canCancel: boolean;
};

export type CreateSupplierOrderPayload = {
  supplierId: number;
  expectedDeliveryDate: string;
  notes: string;
  items: { productCode: string; quantity: number; unitPrice: number; expectedDeliveryDate?: string }[];
};

export type ReceiveSupplierOrderPayload = {
  reason: string;
  items: { lineId: number; quantity: number; unitCost?: number }[];
};

export type UserAccount = {
  id: number;
  username: string;
  role: UserRole;
  roleLabel: string;
  permissions: UserPermission[];
  enabled: boolean;
  disabledAt: string | null;
  disabledBy: string | null;
  disabledReason: string | null;
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

export type StockMovementType = 'INITIAL_BALANCE' | 'ADJUSTMENT_INCREASE' | 'ADJUSTMENT_DECREASE' | 'LOAD' | 'UNLOAD' | 'RETURN' | 'PURCHASE_RECEIPT' | 'PHYSICAL_INVENTORY_INCREASE' | 'PHYSICAL_INVENTORY_DECREASE';

export type StockMovementOrigin = 'LEGACY' | 'MIGRATION_BASELINE' | 'MANUAL_INITIAL_BALANCE' | 'MANUAL_ADJUSTMENT' | 'MANUAL_MOVEMENT' | 'ORDER_FULFILLMENT' | 'CUSTOMER_RETURN' | 'SUPPLIER_ORDER_RECEIPT' | 'PHYSICAL_INVENTORY';

export type StockMovement = {
  id: number;
  timestamp: string;
  actor: string;
  role: string;
  productCode: string;
  productName: string;
  productId?: number;
  type: StockMovementType;
  typeLabel: string;
  quantity: number;
  previousQuantity: number;
  newQuantity: number;
  deltaQuantity: number;
  origin: StockMovementOrigin;
  authoritative: boolean;
  supplierOrderId: number | null;
  supplierOrderReceiptId: number | null;
  supplierOrderReceiptItemId: number | null;
  physicalInventorySessionId: number | null;
  physicalInventoryItemId: number | null;
  unitCost: number | null;
  totalCost: number | null;
  averageCostBefore: number | null;
  averageCostAfter: number | null;
  costedQuantityBefore: number | null;
  costedQuantityAfter: number | null;
  reason: string;
};

export type StockMovementPayload = {
  productCode: string;
  type: 'LOAD' | 'UNLOAD';
  quantity: number;
  reason: string;
};

export type InitialStockPayload = {
  productCode: string;
  quantity: number;
  reason: string;
};

export type InventoryReconciliationStatus = 'BALANCED' | 'MISSING_INITIAL_BALANCE' | 'UNVERIFIED_INITIAL_BALANCE' | 'CHAIN_BROKEN' | 'LEDGER_DRIFT';

export type InventoryReconciliationItem = {
  productId: number;
  productCode: string;
  productName: string;
  physicalQuantity: number;
  ledgerQuantity: number;
  reservedQuantity: number;
  authoritativeMovements: number;
  legacyMovements: number;
  status: InventoryReconciliationStatus;
  statusLabel: string;
};

export type InventoryReconciliation = {
  generatedAt: string;
  totalProducts: number;
  balancedProducts: number;
  anomalousProducts: number;
  orphanedLegacyMovements: number;
  items: InventoryReconciliationItem[];
};

export type PhysicalInventoryStatus = 'OPEN' | 'SUBMITTED' | 'APPROVED' | 'CANCELED';

export type PhysicalInventoryQuery = PageQuery & {
  status?: PhysicalInventoryStatus | 'ALL';
};

export type PhysicalInventoryCapabilities = {
  canCount: boolean;
  canSubmit: boolean;
  canApprove: boolean;
  canCancel: boolean;
};

export type PhysicalInventoryItem = {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  theoreticalQuantitySnapshot: number;
  reservedQuantitySnapshot: number;
  countedQuantity: number | null;
  theoreticalQuantityAtCount: number | null;
  reservedQuantityAtCount: number | null;
  differenceQuantity: number | null;
  countedAt: string | null;
  countedBy: string | null;
  countNote: string | null;
  quantityBeforeApproval: number | null;
  quantityAfterApproval: number | null;
  reservedQuantityAtApproval: number | null;
  compensatedMovementDelta: number | null;
  stockMovementId: number | null;
};

export type PhysicalInventorySession = {
  id: number;
  version: number;
  code: string;
  status: PhysicalInventoryStatus;
  statusLabel: string;
  reason: string;
  createdAt: string;
  createdBy: string;
  createdByRole: string;
  submittedAt: string | null;
  submittedBy: string | null;
  submittedByRole: string | null;
  approvedAt: string | null;
  approvedBy: string | null;
  approvedByRole: string | null;
  approvalReason: string | null;
  canceledAt: string | null;
  canceledBy: string | null;
  cancellationReason: string | null;
  itemCount: number;
  countedItems: number;
  differenceItems: number;
  totalAbsoluteDifference: number;
  capabilities: PhysicalInventoryCapabilities;
  items: PhysicalInventoryItem[];
};

export type CreatePhysicalInventoryPayload = {
  reason: string;
  productCodes: string[];
};

export type PhysicalInventoryCountPayload = {
  countedQuantity: number;
  note?: string;
};

export type PhysicalInventoryDecisionPayload = {
  reason: string;
};

export type Order = {
  id: number;
  code: string;
  customerCode?: string;
  customer: string;
  customerAccountId?: number;
  partnerId?: number;
  customerType: 'SELF_SERVICE' | 'REGISTERED' | 'WALK_IN' | 'LEGACY_UNRESOLVED';
  customerTypeLabel: string;
  ownershipStatus: 'ACCOUNT' | 'PARTNER' | 'UNRESOLVED';
  ownershipStatusLabel: string;
  timestamp: string;
  paymentMethod: string;
  payment: OrderPayment;
  items: OrderItem[];
  returns: OrderReturn[];
  total: number;
  status: 'DRAFT' | 'CONFIRMED' | 'FULFILLED' | 'CANCELED';
  statusLabel: string;
  statusChangedAt: string;
  cancellationReference: string | null;
  cancellationReason: string | null;
  canceledAt: string | null;
  canceledBy: string | null;
  canceledByRole: string | null;
  capabilities: OrderCapabilities;
};

export type OrderCapabilities = {
  canConfirm: boolean;
  canFulfill: boolean;
  canCancel: boolean;
  canRecordReceipt: boolean;
  canRequestReturn: boolean;
  returns: ReturnCapabilities[];
};

export type ReturnCapabilities = {
  returnCode: string;
  canApprove: boolean;
  canReject: boolean;
  canReceive: boolean;
  canRefund: boolean;
};

export type DocumentOrderCapabilities = {
  canCreateInvoice: boolean;
  canCreateCreditNote: boolean;
  hasInvoice: boolean;
  hasCreditNote: boolean;
};

export type OrderOperationalDetail = {
  order: Order;
  documents: DocumentOrderCapabilities;
};

export type PaymentMethod = 'CARD' | 'BANK_TRANSFER' | 'CASH';

export type PaymentStatus = 'UNRECONCILED' | 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'FAILED' | 'CANCELED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';

export type OrderPayment = {
  id: number;
  method: PaymentMethod | 'OTHER';
  methodLabel: string;
  methodDetails: string;
  status: PaymentStatus;
  statusLabel: string;
  requestedAmount: number;
  paidAmount: number | null;
  refundedAmount: number | null;
  netPaidAmount: number | null;
  outstandingAmount: number | null;
  refundableAmount: number | null;
  currency: string;
  createdAt: string;
  updatedAt: string;
  reconciliationRequired: boolean;
  reconciledAt: string | null;
  reconciledBy: string | null;
  reconciledByRole: string | null;
  reconciliationReference: string | null;
  reconciliationReason: string | null;
  transactions: PaymentTransaction[];
};

export type PaymentTransaction = {
  id: number;
  code: string;
  type: 'RECEIPT' | 'REFUND' | 'REVERSAL' | 'RECONCILIATION';
  typeLabel: string;
  amount: number;
  reference: string;
  reason: string;
  returnCode: string | null;
  returnId: number | null;
  cancellationOrderId: number | null;
  reconciliationPaymentId: number | null;
  recordedAt: string;
  recordedBy: string;
  recordedByRole: string;
};

export type FinancialMismatchType =
  | 'PAYMENT_PAID_LEDGER_DRIFT'
  | 'PAYMENT_REFUNDED_LEDGER_DRIFT'
  | 'PAYMENT_INVALID_STATE'
  | 'PAYMENT_INVALID_TIMELINE'
  | 'PAYMENT_INVALID_TRANSACTION_LINK'
  | 'PAYMENT_UNRECONCILED'
  | 'RETURN_REFUNDED_LEDGER_DRIFT'
  | 'RETURN_INVALID_STATE'
  | 'RETURN_INVALID_TIMELINE';

export type FinancialMismatch = {
  type: FinancialMismatchType;
  aggregateType: 'PAYMENT' | 'RETURN';
  aggregateId: number;
  aggregateCode: string;
  orderCode: string;
  materializedAmount: number | null;
  ledgerAmount: number | null;
  detail: string;
};

export type FinancialReconciliation = {
  generatedAt: string;
  balanced: boolean;
  checkedPayments: number;
  checkedReturns: number;
  mismatchCount: number;
  counts: Partial<Record<FinancialMismatchType, number>>;
  mismatches: FinancialMismatch[];
};

export type OrderReturnStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'RECEIVED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';

export type OrderReturn = {
  id: number;
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
  refundTransactions: PaymentTransaction[];
};

export type OrderReturnItem = {
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type ReceiptPayload = { amount: number; reference?: string; reason: string };
export type CancellationPayload = { reference?: string; reason: string };
export type ReturnRequestPayload = { reason: string; items: { productCode: string; quantity: number }[] };
export type ReturnRefundPayload = { amount: number; reference?: string; reason: string };

export type OrderItem = {
  productCode: string;
  productName: string;
  productDescription: string;
  quantity: number;
  returnedOrReservedQuantity: number;
  returnableQuantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type CreateOrderPayload = {
  customer?: string;
  customerCode?: string;
  customerType?: 'SELF_SERVICE' | 'REGISTERED' | 'WALK_IN';
  customerPartnerId?: number;
  walkInCustomerName?: string;
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
  companySnapshotTimeZone: string;
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
  capabilities: {
    canCreateCreditNote: boolean;
  };
};

export type CompanySettings = {
  version: number;
  configured: boolean;
  missingDocumentFields: string[];
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
  timeZone: string;
  defaultVatRate: number;
  invoicePrefix: string;
  creditNotePrefix: string;
  numberPadding: number;
  updatedAt: string;
  updatedBy: string;
};

export type CompanySettingsPayload = Omit<CompanySettings, 'configured' | 'missingDocumentFields' | 'updatedAt' | 'updatedBy'>;

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
  potentialRetailStockValue: number;
  knownInventoryCostValue: number;
  potentialGrossMarginOnCostedStock: number;
  costedUnits: number;
  uncostedUnits: number;
  costCoveragePercentage: number;
  lowStock: number;
  outOfStock: number;
  orders: {
    totalOrders: number;
    draftOrders: number;
    confirmedOrders: number;
    fulfilledOrders: number;
    canceledOrders: number;
    draftOrderValue: number;
    confirmedOrderValue: number;
    fulfilledOrderValue: number;
    grossCollected: number;
    refunded: number;
    netCollected: number;
  };
  recentOrders: Order[];
  recentMovements: StockMovement[];
};

export type CustomerOrderSummary = {
  code: string;
  timestamp: string;
  total: number;
  status: Order['status'];
  statusLabel: string;
  paymentStatus: PaymentStatus;
  paymentStatusLabel: string;
};

export type CustomerDashboardSummary = {
  totalOrders: number;
  draftOrders: number;
  confirmedOrders: number;
  fulfilledOrders: number;
  canceledOrders: number;
  recentOrders: CustomerOrderSummary[];
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
  potentialRetailStockValue: number;
  knownInventoryCostValue: number;
  potentialGrossMarginOnCostedStock: number;
  costedUnits: number;
  uncostedUnits: number;
  costCoveragePercentage: number;
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
    potentialRetailValue: number;
    lastPurchaseCost: number | null;
    averagePurchaseCost: number | null;
    costedQuantity: number;
    uncostedQuantity: number;
    costCoveragePercentage: number;
    knownInventoryCost: number;
    potentialGrossMarginOnCostedStock: number;
    discontinued: boolean;
    stockStatus: 'AVAILABLE' | 'LOW' | 'OUT';
    stockStatusLabel: string;
  }[];
};
