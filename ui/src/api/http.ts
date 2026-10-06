const TOKEN_KEY = 'educational-platform.token';

export class HttpError extends Error {
  status: number;

  constructor(status: number, message?: string) {
    super(message ?? `Request failed with status ${status}`);
    this.name = 'HttpError';
    this.status = status;
  }
}

export const TOKEN_CLEARED_EVENT = 'educational-platform:token-cleared';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function tokenExpiresAt(token: string): number | null {
  try {
    const [, payload] = token.split('.');
    if (!payload) return null;
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number };
    return typeof json.exp === 'number' ? json.exp * 1000 : null;
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const expiresAt = tokenExpiresAt(token);
  return expiresAt !== null && expiresAt <= Date.now();
}

/** Returns the stored token if it is still valid; otherwise clears it and notifies listeners. */
export function getValidToken(): string | null {
  const token = getToken();
  if (!token) return null;
  if (isTokenExpired(token)) {
    clearToken();
    return null;
  }
  return token;
}

export function clearToken() {
  if (localStorage.getItem(TOKEN_KEY) === null) return;
  localStorage.removeItem(TOKEN_KEY);
  window.dispatchEvent(new Event(TOKEN_CLEARED_EVENT));
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    clearToken();
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  parse?: 'json' | 'text';
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  const token = getValidToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  if (!response.ok) {
    if (response.status === 401 && token) {
      clearToken();
    }
    throw new HttpError(response.status);
  }
  if (options.parse === 'text') {
    return (await response.text()) as T;
  }
  return response.json() as Promise<T>;
}

export function get<T>(url: string): Promise<T> {
  return request<T>(url);
}

export function post<T>(url: string, body: unknown, parse: 'json' | 'text' = 'json'): Promise<T> {
  return request<T>(url, { method: 'POST', body, parse });
}
