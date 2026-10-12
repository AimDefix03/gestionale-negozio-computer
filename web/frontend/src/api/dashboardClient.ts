import { request } from './httpClient';
import type { CustomerDashboardSummary, DashboardSummary } from './types';

export function fetchDashboard(): Promise<DashboardSummary> {
  return request<DashboardSummary>('/api/dashboard');
}

export function fetchCustomerDashboard(): Promise<CustomerDashboardSummary> {
  return request<CustomerDashboardSummary>('/api/customer/dashboard');
}
