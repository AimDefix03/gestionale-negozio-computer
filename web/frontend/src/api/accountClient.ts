import { clearSessionToken, fetchAllPages, request, toQueryString } from './httpClient';
import type { AccountQuery, AuthSession, PageResponse, PasswordStrength, UserAccount, UserRole } from './types';

const baseUrl = '/api/accounts';

export function login(username: string, password: string, role: UserRole): Promise<AuthSession> {
  return request<AuthSession>(`${baseUrl}/login`, {
    method: 'POST',
    body: JSON.stringify({ username, password, role })
  });
}

export function renewSession(password: string): Promise<AuthSession> {
  return request<AuthSession>(`${baseUrl}/session/renew`, {
    method: 'POST',
    body: JSON.stringify({ password })
  });
}

export async function logoutSession(): Promise<void> {
  await request<void>(`${baseUrl}/logout`, { method: 'POST' });
  clearSessionToken();
}

export function register(username: string, password: string, role: 'EMPLOYEE' | 'CUSTOMER'): Promise<UserAccount> {
  return request<UserAccount>(`${baseUrl}/register`, {
    method: 'POST',
    body: JSON.stringify({ username, password, role })
  });
}

export function evaluatePassword(password: string): Promise<PasswordStrength> {
  return request<PasswordStrength>(`${baseUrl}/password-strength`, {
    method: 'POST',
    body: JSON.stringify({ password })
  });
}

export function fetchAccountPage(query: AccountQuery = {}): Promise<PageResponse<UserAccount>> {
  return request<PageResponse<UserAccount>>(`${baseUrl}${toQueryString(query)}`);
}

export function fetchAccounts(query: AccountQuery = {}): Promise<UserAccount[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchAccountPage({ ...filters, page, size: pageSize }), size);
}

export function createAccount(username: string, password: string, role: UserRole, reauthPassword: string): Promise<UserAccount> {
  return request<UserAccount>(baseUrl, {
    method: 'POST',
    headers: { 'X-Reauth-Password': reauthPassword },
    body: JSON.stringify({ username, password, role })
  });
}

export async function deleteAccount(username: string, reauthPassword: string): Promise<void> {
  await request<void>(`${baseUrl}/${encodeURIComponent(username)}`, {
    method: 'DELETE',
    headers: { 'X-Reauth-Password': reauthPassword }
  });
}
