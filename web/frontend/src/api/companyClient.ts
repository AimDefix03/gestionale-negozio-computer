import { request } from './httpClient';
import type { CompanySettings, CompanySettingsPayload } from './types';

const baseUrl = '/api/company-settings';

export function fetchCompanySettings(signal?: AbortSignal): Promise<CompanySettings> {
  return request<CompanySettings>(baseUrl, { signal });
}

export function updateCompanySettings(payload: CompanySettingsPayload): Promise<CompanySettings> {
  return request<CompanySettings>(baseUrl, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}
