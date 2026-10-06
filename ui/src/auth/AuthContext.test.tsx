import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearToken, setToken } from '../api/http';
import { AuthProvider, decodeToken, useAuth } from './AuthContext';

const TOKEN_KEY = 'educational-platform.token';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

const STUDENT_TOKEN = makeToken({ sub: 'alice', auth: [{ authority: 'ROLE_STUDENT' }], exp: Date.now() / 1000 + 3600 });
const TEACHER_TOKEN = makeToken({ sub: 'bob', auth: [{ authority: 'ROLE_TEACHER' }], exp: Date.now() / 1000 + 3600 });

describe('decodeToken', () => {
  it('extracts the username and roles', () => {
    const token = makeToken({ sub: 'alice', auth: [{ authority: 'ROLE_STUDENT' }], exp: Date.now() / 1000 + 60 });
    expect(decodeToken(token)).toEqual({ username: 'alice', roles: ['ROLE_STUDENT'] });
  });

  it('defaults to no roles when the auth claim is missing', () => {
    expect(decodeToken(makeToken({ sub: 'alice', exp: Date.now() / 1000 + 60 }))).toEqual({ username: 'alice', roles: [] });
  });

  it('rejects tokens without a subject', () => {
    expect(decodeToken(makeToken({ auth: [], exp: Date.now() / 1000 + 60 }))).toBeNull();
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

function Probe() {
  const { user, isStudent, signIn, signOut } = useAuth();
  return (
    <div>
      <p data-testid="user">{user ? `${user.username}:${user.roles.join(',')}` : 'anonymous'}</p>
      <p data-testid="student">{String(isStudent)}</p>
      <button type="button" onClick={() => void signIn('alice', 'secret').catch(() => undefined)}>
        sign in
      </button>
      <button type="button" onClick={signOut}>
        sign out
      </button>
    </div>
  );
}

function response(status: number, body: string) {
  return { ok: status >= 200 && status < 300, status, text: () => Promise.resolve(body), json: () => Promise.resolve(body) } as Response;
}

describe('AuthProvider', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('restores the signed-in user from a stored token', () => {
    setToken(STUDENT_TOKEN);

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    expect(screen.getByTestId('user')).toHaveTextContent('alice:ROLE_STUDENT');
    expect(screen.getByTestId('student')).toHaveTextContent('true');
  });

  it('flags non-students as such', () => {
    setToken(TEACHER_TOKEN);

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    expect(screen.getByTestId('user')).toHaveTextContent('bob:ROLE_TEACHER');
    expect(screen.getByTestId('student')).toHaveTextContent('false');
  });

  it('drops an unusable stored token on start-up', () => {
    localStorage.setItem(TOKEN_KEY, makeToken({ sub: 'alice', exp: Date.now() / 1000 - 60 }));

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('signs in, stores the token and exposes the decoded user', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(200, STUDENT_TOKEN));
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'sign in' }));

    expect(await screen.findByText('alice:ROLE_STUDENT')).toBeInTheDocument();
    expect(localStorage.getItem(TOKEN_KEY)).toBe(STUDENT_TOKEN);
  });

  it('does not store a token the backend returned that cannot be decoded', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(response(200, 'garbage'));
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'sign in' }));

    await act(() => Promise.resolve());
    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('signs out by clearing the token and the user', () => {
    setToken(STUDENT_TOKEN);
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'sign out' }));

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
    expect(screen.getByTestId('student')).toHaveTextContent('false');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('forgets the user when the token is cleared outside React (e.g. after a 401)', () => {
    setToken(STUDENT_TOKEN);
    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );
    expect(screen.getByTestId('user')).toHaveTextContent('alice:ROLE_STUDENT');

    act(() => clearToken());

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
  });

  it('useAuth throws outside of an AuthProvider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => render(<Probe />)).toThrow('useAuth must be used within an AuthProvider');
  });
});
