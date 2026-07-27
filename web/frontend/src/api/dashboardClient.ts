import { request } from './httpClient';
import type { DashboardSummary } from './types';

export function fetchDashboard(): Promise<DashboardSummary> {
  return request<DashboardSummary>('/api/dashboard');
}
