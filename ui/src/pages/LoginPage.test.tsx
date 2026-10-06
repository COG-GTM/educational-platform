import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getToken, setToken } from '../api/http';
import { AuthProvider } from '../auth/AuthContext';
import LoginPage from './LoginPage';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const STUDENT_TOKEN = makeToken({
  sub: 'alice',
  auth: [{ authority: 'ROLE_STUDENT' }],
  exp: Math.floor(Date.now() / 1000) + 3600,
});

function response(status: number, body: string) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(JSON.parse(body)),
    text: () => Promise.resolve(body),
  } as Response;
}

function mockSignIn(status: number, body = STUDENT_TOKEN) {
  const calls: { url: string; init: RequestInit | undefined }[] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
    calls.push({ url: String(input), init });
    return Promise.resolve(response(status, body));
  });
  return calls;
}

function renderPage(path = '/login?redirect=/courses/abc') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/courses/abc" element={<h2>Course page</h2>} />
          <Route path="/catalog" element={<h2>Catalog page</h2>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

function submit(username = '  alice  ', password = 'secret') {
  fireEvent.change(screen.getByLabelText('Username'), { target: { value: username } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
}

describe('LoginPage', () => {
  beforeEach(() => setToken(null));
  afterEach(() => vi.restoreAllMocks());

  it('signs in with trimmed credentials, stores the token and returns to the requested page', async () => {
    const calls = mockSignIn(200);
    renderPage();

    submit();

    expect(await screen.findByRole('heading', { name: 'Course page' })).toBeInTheDocument();
    expect(getToken()).toBe(STUDENT_TOKEN);
    expect(calls).toHaveLength(1);
    expect(calls[0].url).toBe('/api/users/sign-in');
    expect(calls[0].init?.method).toBe('POST');
    expect(calls[0].init?.body).toBe(JSON.stringify({ username: 'alice', password: 'secret' }));
  });

  it('falls back to the catalog when the redirect is missing or unsafe', async () => {
    mockSignIn(200);
    renderPage('/login?redirect=//evil.example');

    expect(screen.getByRole('link', { name: 'Back' })).toHaveAttribute('href', '/catalog');
    submit();

    expect(await screen.findByRole('heading', { name: 'Catalog page' })).toBeInTheDocument();
  });

  it('links Back to a safe redirect target', () => {
    mockSignIn(200);
    renderPage();

    expect(screen.getByRole('link', { name: 'Back' })).toHaveAttribute('href', '/courses/abc');
  });

  it.each([401, 422])('reports wrong credentials on %i and stays on the page without a token', async (status) => {
    mockSignIn(status, '{}');
    renderPage();

    submit();

    expect(await screen.findByRole('alert')).toHaveTextContent('Wrong username or password.');
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled();
    expect(getToken()).toBeNull();
  });

  it('reports a generic failure on server errors', async () => {
    mockSignIn(500, '{}');
    renderPage();

    submit();

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not sign you in right now. Please try again.');
    expect(getToken()).toBeNull();
  });

  it('rejects a response that is not a usable session token', async () => {
    mockSignIn(200, 'not-a-jwt');
    renderPage();

    submit();

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not sign you in right now. Please try again.');
    expect(getToken()).toBeNull();
  });

  it('disables the button while the request is in flight', async () => {
    let resolve: (value: Response) => void = () => undefined;
    vi.spyOn(globalThis, 'fetch').mockImplementation(() => new Promise<Response>((r) => (resolve = r)));
    renderPage();

    submit();

    expect(await screen.findByRole('button', { name: 'Signing in…' })).toBeDisabled();
    resolve(response(200, STUDENT_TOKEN));
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Course page' })).toBeInTheDocument());
  });
});
