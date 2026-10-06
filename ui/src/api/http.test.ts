import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  clearToken,
  get,
  getToken,
  getValidToken,
  HttpError,
  isTokenExpired,
  post,
  setToken,
  TOKEN_CLEARED_EVENT,
  tokenExpiresAt,
} from './http';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const NOW_SECONDS = Math.floor(Date.now() / 1000);
const VALID_TOKEN = makeToken({ sub: 'alice', exp: NOW_SECONDS + 3600 });
const EXPIRED_TOKEN = makeToken({ sub: 'alice', exp: NOW_SECONDS - 60 });

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  } as Response;
}

function mockFetch(status: number, body?: unknown, onCall?: () => void) {
  const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(() => {
    onCall?.();
    return Promise.resolve(response(status, body));
  });
  return spy;
}

function requestInit(spy: { mock: { calls: unknown[][] } }): RequestInit {
  return spy.mock.calls[0][1] as RequestInit;
}

describe('http token helpers', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('tokenExpiresAt reads exp in milliseconds and tolerates base64url payloads', () => {
    expect(tokenExpiresAt(VALID_TOKEN)).toBe((NOW_SECONDS + 3600) * 1000);
    const urlSafe = VALID_TOKEN.replace(/\+/g, '-').replace(/\//g, '_');
    expect(tokenExpiresAt(urlSafe)).toBe((NOW_SECONDS + 3600) * 1000);
  });

  it('tokenExpiresAt returns null for tokens without exp or malformed tokens', () => {
    expect(tokenExpiresAt(makeToken({ sub: 'alice' }))).toBeNull();
    expect(tokenExpiresAt('not-a-token')).toBeNull();
    expect(tokenExpiresAt('a.%%%.c')).toBeNull();
  });

  it('isTokenExpired is true only for a past exp', () => {
    expect(isTokenExpired(EXPIRED_TOKEN)).toBe(true);
    expect(isTokenExpired(VALID_TOKEN)).toBe(false);
    expect(isTokenExpired(makeToken({ sub: 'alice' }))).toBe(false);
  });

  it('setToken stores the token and setToken(null) removes it', () => {
    setToken(VALID_TOKEN);
    expect(getToken()).toBe(VALID_TOKEN);
    setToken(null);
    expect(getToken()).toBeNull();
  });

  it('clearToken removes a stored token and notifies listeners once', () => {
    const listener = vi.fn();
    window.addEventListener(TOKEN_CLEARED_EVENT, listener);
    setToken(VALID_TOKEN);

    clearToken();
    clearToken();

    window.removeEventListener(TOKEN_CLEARED_EVENT, listener);
    expect(getToken()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
  });

  it('clearToken does not notify when nothing was stored', () => {
    const listener = vi.fn();
    window.addEventListener(TOKEN_CLEARED_EVENT, listener);

    clearToken();

    window.removeEventListener(TOKEN_CLEARED_EVENT, listener);
    expect(listener).not.toHaveBeenCalled();
  });

  it('getValidToken returns a stored unexpired token', () => {
    setToken(VALID_TOKEN);
    expect(getValidToken()).toBe(VALID_TOKEN);
    expect(getToken()).toBe(VALID_TOKEN);
  });

  it('getValidToken clears an expired token and notifies listeners', () => {
    const listener = vi.fn();
    window.addEventListener(TOKEN_CLEARED_EVENT, listener);
    setToken(EXPIRED_TOKEN);

    const result = getValidToken();

    window.removeEventListener(TOKEN_CLEARED_EVENT, listener);
    expect(result).toBeNull();
    expect(getToken()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
  });

  it('getValidToken returns null when no token is stored', () => {
    expect(getValidToken()).toBeNull();
  });
});

describe('http request', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('GET sends Accept without Authorization or body when anonymous', async () => {
    const spy = mockFetch(200, { ok: true });

    const result = await get<{ ok: boolean }>('/api/thing');

    expect(result).toEqual({ ok: true });
    const init = requestInit(spy);
    expect(spy.mock.calls[0][0]).toBe('/api/thing');
    expect(init.method).toBe('GET');
    expect(init.body).toBeUndefined();
    expect(init.headers).toEqual({ Accept: 'application/json' });
  });

  it('adds a bearer header when a valid token is stored', async () => {
    setToken(VALID_TOKEN);
    const spy = mockFetch(200, {});

    await get('/api/thing');

    expect((requestInit(spy).headers as Record<string, string>).Authorization).toBe(`Bearer ${VALID_TOKEN}`);
  });

  it('does not send an expired token and clears it before the request', async () => {
    setToken(EXPIRED_TOKEN);
    const spy = mockFetch(200, {});

    await get('/api/thing');

    expect(requestInit(spy).headers).toEqual({ Accept: 'application/json' });
    expect(getToken()).toBeNull();
  });

  it('POST serialises the body as JSON with a content type', async () => {
    const spy = mockFetch(201, { uuid: 'x' });

    const result = await post<{ uuid: string }>('/api/things', { student: 'alice' });

    expect(result).toEqual({ uuid: 'x' });
    const init = requestInit(spy);
    expect(init.method).toBe('POST');
    expect(init.body).toBe(JSON.stringify({ student: 'alice' }));
    expect((init.headers as Record<string, string>)['Content-Type']).toBe('application/json');
  });

  it('POST with parse=text returns the raw response text', async () => {
    mockFetch(200, 'jwt-token');

    await expect(post<string>('/api/users/sign-in', { username: 'a', password: 'b' }, 'text')).resolves.toBe('jwt-token');
  });

  it('rejects with an HttpError carrying the status on non-2xx responses', async () => {
    mockFetch(404, { message: 'not found' });

    const error = await get('/api/missing').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(HttpError);
    expect((error as HttpError).status).toBe(404);
    expect((error as HttpError).name).toBe('HttpError');
    expect((error as HttpError).message).toBe('Request failed with status 404');
  });

  it('HttpError keeps a custom message when given', () => {
    const error = new HttpError(500, 'boom');
    expect(error.message).toBe('boom');
    expect(error.status).toBe(500);
  });

  it('clears the current token and notifies listeners when the backend answers 401', async () => {
    const listener = vi.fn();
    window.addEventListener(TOKEN_CLEARED_EVENT, listener);
    setToken(VALID_TOKEN);
    mockFetch(401);

    await expect(get('/api/private')).rejects.toMatchObject({ status: 401 });

    window.removeEventListener(TOKEN_CLEARED_EVENT, listener);
    expect(getToken()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
  });

  it('does not touch storage on an anonymous 401', async () => {
    const listener = vi.fn();
    window.addEventListener(TOKEN_CLEARED_EVENT, listener);
    mockFetch(401);

    await expect(get('/api/private')).rejects.toMatchObject({ status: 401 });

    window.removeEventListener(TOKEN_CLEARED_EVENT, listener);
    expect(listener).not.toHaveBeenCalled();
  });

  it('keeps a token that replaced the rejected one while the request was in flight', async () => {
    const replacement = makeToken({ sub: 'bob', exp: NOW_SECONDS + 7200 });
    setToken(VALID_TOKEN);
    mockFetch(401, undefined, () => setToken(replacement));

    await expect(get('/api/private')).rejects.toMatchObject({ status: 401 });

    expect(getToken()).toBe(replacement);
  });

  it('does not clear the token on non-401 failures', async () => {
    setToken(VALID_TOKEN);
    mockFetch(500);

    await expect(get('/api/private')).rejects.toMatchObject({ status: 500 });

    expect(getToken()).toBe(VALID_TOKEN);
  });
});
