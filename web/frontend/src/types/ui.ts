import { Product, UserRole } from '../api';

export type View = 'dashboard' | 'catalog' | 'partners' | 'inventory' | 'orders' | 'documents' | 'reports' | 'accounts' | 'company' | 'audit' | 'monitoring';

export type CartItem = {
  product: Product;
  quantity: number;
};

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
  role: UserRole;
};

export type MovementFormState = {
  productCode: string;
  type: 'LOAD' | 'UNLOAD';
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
  inventoryValue: number;
  lowStock: number;
  outOfStock: number;
  orders: number;
  revenue: number;
};
