import { clearSessionToken, fetchAllPages, request, toQueryString } from './httpClient';
import type { AccountQuery, AuthSession, PageResponse, PasswordStrength, UserAccount, UserRole } from './types';

const baseUrl = '/api/accounts';

export function login(username: string, password: string): Promise<AuthSession> {
  return request<AuthSession>(`${baseUrl}/login`, {
    method: 'POST',
    body: JSON.stringify({ username, password })
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

export function register(username: string, password: string): Promise<UserAccount> {
  return request<UserAccount>(`${baseUrl}/register`, {
    method: 'POST',
    body: JSON.stringify({ username, password })
  });
}

export function evaluatePassword(password: string): Promise<PasswordStrength> {
  return request<PasswordStrength>(`${baseUrl}/password-strength`, {
    method: 'POST',
    body: JSON.stringify({ password })
  });
}

export function fetchAccountPage(query: AccountQuery = {}, signal?: AbortSignal): Promise<PageResponse<UserAccount>> {
  return request<PageResponse<UserAccount>>(`${baseUrl}${toQueryString(query)}`, { signal });
}

export function fetchAccounts(query: AccountQuery = {}, signal?: AbortSignal): Promise<UserAccount[]> {
  const { page: _page, size, ...filters } = query;
  return fetchAllPages((page, pageSize) => fetchAccountPage({ ...filters, page, size: pageSize }, signal), size);
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

export async function changeOwnPassword(currentPassword: string, newPassword: string): Promise<void> {
  await request<void>(`${baseUrl}/me/password`, {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword })
  });
}

export function disableAccount(username: string, reason: string, reauthPassword: string): Promise<UserAccount> {
  return accountAction(username, 'disable', { reason }, reauthPassword);
}

export function enableAccount(username: string, reason: string, reauthPassword: string): Promise<UserAccount> {
  return accountAction(username, 'enable', { reason }, reauthPassword);
}

export async function resetAccountPassword(username: string, newPassword: string, reason: string, reauthPassword: string): Promise<void> {
  await accountAction<void>(username, 'password-reset', { newPassword, reason }, reauthPassword);
}

export async function revokeAccountSessions(username: string, reason: string, reauthPassword: string): Promise<void> {
  await accountAction<void>(username, 'sessions/revoke', { reason }, reauthPassword);
}

export function changeAccountRole(username: string, role: UserRole, reason: string, reauthPassword: string): Promise<UserAccount> {
  return accountAction(username, 'role', { role, reason }, reauthPassword);
}

function accountAction<T = UserAccount>(username: string, action: string, body: unknown, reauthPassword: string): Promise<T> {
  return request<T>(`${baseUrl}/${encodeURIComponent(username)}/${action}`, {
    method: 'POST',
    headers: { 'X-Reauth-Password': reauthPassword },
    body: JSON.stringify(body)
  });
}
