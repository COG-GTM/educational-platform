import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchCatalog, fetchCatalogFacets } from './catalog';
import { getToken, HttpError, setToken, TOKEN_CLEARED_EVENT } from './http';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const STUDENT_TOKEN = makeToken({ sub: 'alice', exp: Math.floor(Date.now() / 1000) + 3600 });

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as Response;
}

function mockFetch(status: number, body?: unknown) {
  const calls: { url: string; init: RequestInit | undefined }[] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
    calls.push({ url: String(input), init });
    return Promise.resolve(response(status, body));
  });
  return calls;
}

function headers(init: RequestInit | undefined): Record<string, string> {
  return (init?.headers ?? {}) as Record<string, string>;
}

function queryOf(url: string): URLSearchParams {
  return new URL(url, 'http://localhost').searchParams;
}

describe('catalog api', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('fetchCatalog defaults to the first page of 12 and omits unset filters', async () => {
    const page = { items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 };
    const calls = mockFetch(200, page);

    await expect(fetchCatalog({})).resolves.toEqual(page);

    expect(calls).toHaveLength(1);
    expect(calls[0].url.startsWith('/api/courses?')).toBe(true);
    expect(Object.fromEntries(queryOf(calls[0].url))).toEqual({ page: '0', size: '12' });
  });

  it('fetchCatalog forwards every filter, sort and paging parameter', async () => {
    const calls = mockFetch(200, { items: [] });

    await fetchCatalog({
      search: 'spring boot',
      category: 'Programming',
      teacher: 'bob',
      minRating: '4',
      sort: 'NEWEST',
      page: 2,
      size: 5,
    });

    expect(Object.fromEntries(queryOf(calls[0].url))).toEqual({
      search: 'spring boot',
      category: 'Programming',
      teacher: 'bob',
      minRating: '4',
      sort: 'NEWEST',
      page: '2',
      size: '5',
    });
  });

  it('fetchCatalog drops empty-string filters instead of sending blank parameters', async () => {
    const calls = mockFetch(200, { items: [] });

    await fetchCatalog({ search: '', category: '', teacher: '', minRating: '', page: 0 });

    expect(Object.fromEntries(queryOf(calls[0].url))).toEqual({ page: '0', size: '12' });
  });

  it('fetchCatalog url-encodes filter values so they cannot break the query string', async () => {
    const calls = mockFetch(200, { items: [] });

    await fetchCatalog({ search: 'a&b=c', category: 'Data Science' });

    const query = queryOf(calls[0].url);
    expect(query.get('search')).toBe('a&b=c');
    expect(query.get('category')).toBe('Data Science');
    expect(calls[0].url).toContain('search=a%26b%3Dc');
  });

  it('fetchCatalog is an anonymous JSON GET without a body when no token is stored', async () => {
    const calls = mockFetch(200, { items: [] });

    await fetchCatalog({});

    expect(calls[0].init?.method).toBe('GET');
    expect(calls[0].init?.body).toBeUndefined();
    expect(headers(calls[0].init)).toEqual({ Accept: 'application/json' });
  });

  it('fetchCatalog sends the stored bearer token through the shared http client', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockFetch(200, { items: [] });

    await fetchCatalog({});

    expect(headers(calls[0].init).Authorization).toBe(`Bearer ${STUDENT_TOKEN}`);
  });

  it('fetchCatalog surfaces a failed response as an HttpError carrying the status', async () => {
    mockFetch(500, { message: 'boom' });

    await expect(fetchCatalog({})).rejects.toBeInstanceOf(HttpError);
    await expect(fetchCatalog({})).rejects.toMatchObject({ name: 'HttpError', status: 500 });
  });

  it('fetchCatalog clears a rejected token on 401 like every other api call', async () => {
    setToken(STUDENT_TOKEN);
    const cleared = vi.fn();
    window.addEventListener(TOKEN_CLEARED_EVENT, cleared);
    mockFetch(401, { message: 'expired' });

    try {
      await expect(fetchCatalog({})).rejects.toMatchObject({ status: 401 });

      expect(getToken()).toBeNull();
      expect(cleared).toHaveBeenCalledTimes(1);
    } finally {
      window.removeEventListener(TOKEN_CLEARED_EVENT, cleared);
    }
  });

  it('fetchCatalogFacets GETs the facets resource and returns the parsed facets', async () => {
    const facets = { categories: ['Programming'], teachers: ['bob'] };
    const calls = mockFetch(200, facets);

    await expect(fetchCatalogFacets()).resolves.toEqual(facets);

    expect(calls).toHaveLength(1);
    expect(calls[0].url).toBe('/api/courses/catalog-facets');
    expect(calls[0].init?.method).toBe('GET');
    expect(headers(calls[0].init)).toEqual({ Accept: 'application/json' });
  });

  it('fetchCatalogFacets surfaces a failed response as an HttpError', async () => {
    mockFetch(503, { message: 'unavailable' });

    await expect(fetchCatalogFacets()).rejects.toMatchObject({ name: 'HttpError', status: 503 });
  });
});
