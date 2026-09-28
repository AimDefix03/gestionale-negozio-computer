import { SellableProduct, UserRole } from '../api';

export type View = 'dashboard' | 'catalog' | 'sales' | 'partners' | 'inventory' | 'orders' | 'purchases' | 'documents' | 'reports' | 'accounts' | 'company' | 'audit' | 'monitoring';

export type CartItem = {
  product: SellableProduct;
  quantity: number;
  maximumQuantity?: number;
};

export type SalesCustomerMode = 'REGISTERED' | 'WALK_IN';

export type MenuItem = {
  label: string;
  description: string;
  view?: View;
  action?: () => void;
  disabled?: boolean;
};

export type MenuGroup = {
  id: string;
  label: string;
  items: MenuItem[];
};

export type AuthMode = 'login' | 'register';

export type AuthFormState = {
  username: string;
  password: string;
};

export type MovementFormState = {
  productCode: string;
  operation: 'INITIAL_BALANCE' | 'LOAD' | 'UNLOAD';
  quantity: string;
  reason: string;
};

export type AccountFormState = {
  username: string;
  password: string;
  role: UserRole;
};

export type DashboardStats = {
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
};
