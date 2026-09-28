import type { PageResponse } from './types';

type ApiError = {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  details?: string[];
  path?: string;
  requestId?: string;
};

let sessionToken = '';
const sessionExpiredListeners = new Set<(error: SessionExpiredError) => void>();
const pendingIdempotencyIntents = new Map<string, IdempotencyIntent>();
const idempotencyIntentTtlMs = 24 * 60 * 60 * 1000;

type IdempotencyIntent = {
  key: string;
  createdAt: number;
};

export type DownloadedFile = {
  blob: Blob;
  filename: string;
};

export class SessionExpiredError extends Error {
  status = 401;
  code = 'AUTH_UNAUTHORIZED';
  details: string[] = [];
  requestId?: string;

  constructor(message = 'Sessione scaduta. Effettua di nuovo il login.') {
    super(message);
    this.name = 'SessionExpiredError';
  }
}

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code = 'REQUEST_FAILED',
    public readonly details: string[] = [],
    public readonly requestId?: string
  ) {
    super(message);
    this.name = 'ApiRequestError';
  }
}

export function setSessionToken(token: string) {
  sessionToken = token;
}

export function clearSessionToken() {
  sessionToken = '';
  clearPendingIdempotencyIntents();
}

export function subscribeToSessionExpiration(listener: (error: SessionExpiredError) => void): () => void {
  sessionExpiredListeners.add(listener);
  return () => sessionExpiredListeners.delete(listener);
}

export async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const headers = requestHeaders(options.headers);
  const response = await fetch(url, { ...options, headers });
  await requireSuccess(response, headers);

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export async function requestBlob(url: string, options: RequestInit = {}): Promise<DownloadedFile> {
  const headers = requestHeaders(options.headers);
  const response = await fetch(url, { ...options, headers });
  await requireSuccess(response, headers);
  return {
    blob: await response.blob(),
    filename: parseDownloadFilename(response.headers.get('Content-Disposition'))
  };
}

export async function requestIdempotent<T>(
  url: string,
  scope: string,
  intentPayload: unknown,
  options: RequestInit = {}
): Promise<T> {
  purgeExpiredIdempotencyIntents();
  const signature = `${scope}\n${url}\n${stableSerialize(intentPayload)}`;
  const intent = pendingIdempotencyIntents.get(signature) ?? createIdempotencyIntent(scope);
  pendingIdempotencyIntents.set(signature, intent);
  const headers = new Headers(options.headers);
  headers.set('Idempotency-Key', intent.key);

  try {
    const response = await request<T>(url, { ...options, headers });
    clearIntent(signature, intent);
    return response;
  } catch (error) {
    if (isDefinitiveFailure(error)) {
      clearIntent(signature, intent);
    }
    throw error;
  }
}

export function saveDownloadedFile(file: DownloadedFile) {
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = sanitizeFilename(file.filename);
  link.rel = 'noopener';
  link.hidden = true;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 0);
}

export function parseDownloadFilename(contentDisposition: string | null, fallback = 'download'): string {
  if (!contentDisposition) return sanitizeFilename(fallback);

  const encoded = contentDisposition.match(/filename\*\s*=\s*([^']*)'[^']*'([^;]+)/i)?.[2];
  if (encoded) {
    try {
      return sanitizeFilename(decodeURIComponent(stripQuotes(encoded.trim())), fallback);
    } catch {
      return sanitizeFilename(fallback);
    }
  }

  const quoted = contentDisposition.match(/filename\s*=\s*"((?:\\.|[^"])*)"/i)?.[1];
  if (quoted) return sanitizeFilename(quoted.replace(/\\(.)/g, '$1'), fallback);

  const plain = contentDisposition.match(/filename\s*=\s*([^;]+)/i)?.[1];
  return sanitizeFilename(plain ? stripQuotes(plain.trim()) : fallback, fallback);
}

export function sanitizeFilename(filename: string, fallback = 'download'): string {
  const normalized = filename
    .normalize('NFKC')
    .replace(/[\u0000-\u001f\u007f]/g, '')
    .replace(/[\\/:*?"<>|]/g, '_')
    .replace(/^\.+/, '')
    .trim()
    .slice(0, 180);
  return normalized && normalized !== '.' && normalized !== '..' ? normalized : fallback;
}

export function clearPendingIdempotencyIntents() {
  pendingIdempotencyIntents.clear();
}

export async function fetchAllPages<T>(loader: (page: number, size: number) => Promise<PageResponse<T>>, requestedSize?: number): Promise<T[]> {
  const size = requestedSize ?? 200;
  const firstPage = await loader(0, size);
  const content = [...firstPage.content];
  for (let page = 1; page < firstPage.totalPages; page += 1) {
    const nextPage = await loader(page, size);
    content.push(...nextPage.content);
  }
  return content;
}

export function toQueryString(query: Record<string, string | number | boolean | undefined | null>): string {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '' || value === 'ALL') return;
    params.set(key, String(value));
  });
  const serialized = params.toString();
  return serialized ? `?${serialized}` : '';
}

function createRequestId(): string {
  const cryptoApi = globalThis.crypto;
  if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID();
  if (typeof cryptoApi?.getRandomValues !== 'function') {
    throw new Error('Generatore crittografico non disponibile.');
  }
  const bytes = cryptoApi.getRandomValues(new Uint8Array(16));
  const randomPart = Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('');
  return `web-${randomPart}`;
}

function createIdempotencyIntent(scope: string): IdempotencyIntent {
  return {
    key: `${scope}-${createRequestId()}`,
    createdAt: Date.now()
  };
}

function clearIntent(signature: string, intent: IdempotencyIntent) {
  if (pendingIdempotencyIntents.get(signature) === intent) {
    pendingIdempotencyIntents.delete(signature);
  }
}

function purgeExpiredIdempotencyIntents() {
  const oldestAllowed = Date.now() - idempotencyIntentTtlMs;
  pendingIdempotencyIntents.forEach((intent, signature) => {
    if (intent.createdAt < oldestAllowed) {
      pendingIdempotencyIntents.delete(signature);
    }
  });
}

function isDefinitiveFailure(error: unknown): boolean {
  if (error instanceof SessionExpiredError) return true;
  if (!(error instanceof ApiRequestError)) return false;
  if (error.code === 'IDEMPOTENCY_IN_PROGRESS') return false;
  return error.status >= 400 && error.status < 500;
}

function stableSerialize(value: unknown): string {
  const serialized = JSON.stringify(canonicalize(value));
  return serialized ?? String(value);
}

function canonicalize(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(canonicalize);
  if (value === null || typeof value !== 'object') return value;
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .filter(([, entry]) => entry !== undefined)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([key, entry]) => [key, canonicalize(entry)])
  );
}

function requestHeaders(initial?: HeadersInit): Headers {
  const headers = new Headers(initial);
  headers.set('Content-Type', 'application/json');
  headers.set('X-Request-Id', headers.get('X-Request-Id') ?? createRequestId());
  if (sessionToken) headers.set('X-Session-Token', sessionToken);
  return headers;
}

async function requireSuccess(response: Response, requestHeaders: Headers): Promise<void> {
  if (response.ok) return;
  const error = await safeJson<ApiError>(response);
  const details = error?.details ?? [];
  const detailsText = details.length ? ` ${details.join(' ')}` : '';
  const message = `${error?.message ?? 'Operazione non riuscita.'}${detailsText}`;
  const requestId = error?.requestId ?? response.headers.get('X-Request-Id') ?? requestHeaders.get('X-Request-Id') ?? undefined;
  if (response.status === 401) {
    const sessionError = new SessionExpiredError(message || 'Sessione scaduta.');
    sessionError.code = error?.code ?? 'AUTH_UNAUTHORIZED';
    sessionError.details = details;
    sessionError.requestId = requestId;
    if (sessionToken) {
      sessionExpiredListeners.forEach((listener) => listener(sessionError));
    }
    throw sessionError;
  }
  throw new ApiRequestError(message, response.status, error?.code, details, requestId);
}

function stripQuotes(value: string): string {
  return value.replace(/^"|"$/g, '');
}

async function safeJson<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T;
  } catch {
    return null;
  }
}
