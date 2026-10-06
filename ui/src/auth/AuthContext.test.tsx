import { describe, expect, it } from 'vitest';
import { decodeToken } from './AuthContext';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

describe('decodeToken', () => {
  it('extracts the username and roles', () => {
    const token = makeToken({ sub: 'alice', auth: [{ authority: 'ROLE_STUDENT' }], exp: Date.now() / 1000 + 60 });
    expect(decodeToken(token)).toEqual({ username: 'alice', roles: ['ROLE_STUDENT'] });
  });

  it('rejects expired tokens', () => {
    const token = makeToken({ sub: 'alice', auth: [], exp: Date.now() / 1000 - 60 });
    expect(decodeToken(token)).toBeNull();
  });

  it('rejects malformed tokens', () => {
    expect(decodeToken('not-a-token')).toBeNull();
    expect(decodeToken('a.b.c')).toBeNull();
  });
});
