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
    filename: filenameFrom(response.headers.get('Content-Disposition')) ?? 'download'
  };
}

export function saveDownloadedFile(file: DownloadedFile) {
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = file.filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function idempotencyHeaders(scope: string): HeadersInit {
  return { 'Idempotency-Key': `${scope}-${createRequestId()}` };
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
    throw sessionError;
  }
  throw new ApiRequestError(message, response.status, error?.code, details, requestId);
}

function filenameFrom(contentDisposition: string | null): string | null {
  if (!contentDisposition) return null;
  const encoded = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) return decodeURIComponent(encoded.replace(/^"|"$/g, ''));
  return contentDisposition.match(/filename="?([^";]+)"?/i)?.[1] ?? null;
}

async function safeJson<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T;
  } catch {
    return null;
  }
}
