import { request } from './httpClient';
import type { CompanySettings, CompanySettingsPayload } from './types';

const baseUrl = '/api/company-settings';

export function fetchCompanySettings(): Promise<CompanySettings> {
  return request<CompanySettings>(baseUrl);
}

export function updateCompanySettings(payload: CompanySettingsPayload): Promise<CompanySettings> {
  return request<CompanySettings>(baseUrl, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}
