import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { signIn } from './auth';

const TOKEN = 'header.payload.signature';

describe('auth api', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('signIn POSTs the credentials as JSON and returns the raw token text', async () => {
    const calls: { url: string; init: RequestInit | undefined }[] = [];
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      calls.push({ url: String(input), init });
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => Promise.resolve(TOKEN),
        json: () => Promise.reject(new SyntaxError('token is not JSON')),
      } as Response);
    });

    await expect(signIn('alice', 's3cret')).resolves.toBe(TOKEN);

    expect(calls).toHaveLength(1);
    expect(calls[0].url).toBe('/api/users/sign-in');
    expect(calls[0].init?.method).toBe('POST');
    expect(JSON.parse(String(calls[0].init?.body))).toEqual({ username: 'alice', password: 's3cret' });
    const headers = calls[0].init?.headers as Record<string, string>;
    expect(headers['Content-Type']).toBe('application/json');
    expect(headers.Authorization).toBeUndefined();
  });

  it('signIn rejects with the unauthorized status for wrong credentials', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 401,
      text: () => Promise.resolve(''),
      json: () => Promise.resolve({}),
    } as Response);

    await expect(signIn('alice', 'wrong')).rejects.toMatchObject({ name: 'HttpError', status: 401 });
  });
});
