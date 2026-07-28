import { request } from './httpClient';
import type { SystemStatus } from './types';

export function fetchSystemStatus(): Promise<SystemStatus> {
  return request<SystemStatus>('/api/system/status');
}
