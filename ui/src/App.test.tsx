import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { getToken, setToken } from './api/http';

const COURSE_UUID = '123e4567-e89b-12d3-a456-426655440002';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const STUDENT_TOKEN = makeToken({
  sub: 'alice',
  auth: [{ authority: 'ROLE_STUDENT' }],
  exp: Math.floor(Date.now() / 1000) + 3600,
});

const course = {
  uuid: COURSE_UUID,
  name: 'Java Basics',
  description: 'Learn Java from scratch',
  category: 'Programming',
  teacherName: 'teacher-bob',
  rating: 4.5,
  numberOfStudents: 3,
  publishedDate: '2026-09-01T10:00:00',
  curriculumItems: [],
};

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  } as Response;
}

function mockApi() {
  const calls: string[] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = String(input);
    calls.push(url);
    if (url.startsWith('/api/courses/catalog-facets')) return Promise.resolve(response(200, { categories: [], teachers: [] }));
    if (url.startsWith('/api/courses?')) {
      return Promise.resolve(response(200, { items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 }));
    }
    if (url.endsWith('/reviews/summary')) return Promise.resolve(response(200, { averageRating: 0, totalReviews: 0, ratingCounts: {} }));
    if (url.endsWith('/reviews')) return Promise.resolve(response(200, []));
    if (url.endsWith('/course-enrollments')) return Promise.resolve(response(200, []));
    if (url === `/api/courses/${COURSE_UUID}`) return Promise.resolve(response(200, course));
    return Promise.resolve(response(404, { message: 'not found' }));
  });
  return calls;
}

function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  );
}

describe('App shell', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('offers a sign-in link that returns the visitor to the current page', async () => {
    mockApi();

    renderApp(`/courses/${COURSE_UUID}`);

    const nav = screen.getByRole('navigation', { name: 'Main' });
    const signIn = await waitFor(() => screen.getByRole('link', { name: 'Sign in' }));
    expect(nav).toContainElement(signIn);
    expect(signIn).toHaveAttribute('href', `/login?redirect=${encodeURIComponent(`/courses/${COURSE_UUID}`)}`);
    expect(within(nav).getByRole('link', { name: 'Catalog' })).toHaveAttribute('href', '/catalog');
    expect(screen.queryByText(/Signed in as/)).not.toBeInTheDocument();
  });

  it('shows the signed-in user and signs out from the header', async () => {
    mockApi();
    setToken(STUDENT_TOKEN);

    renderApp('/catalog');

    expect(screen.getByText('Signed in as alice')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Sign in' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));

    expect(getToken()).toBeNull();
    expect(screen.queryByText(/Signed in as/)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login?redirect=%2Fcatalog');
  });

  it('routes /courses/:uuid to the course detail page', async () => {
    const calls = mockApi();

    renderApp(`/courses/${COURSE_UUID}`);

    expect(await screen.findByRole('heading', { name: 'Java Basics' })).toBeInTheDocument();
    expect(calls).toContain(`/api/courses/${COURSE_UUID}`);
  });

  it('routes /courses/:uuid/reviews to the reviews page', async () => {
    mockApi();

    renderApp(`/courses/${COURSE_UUID}/reviews`);

    expect(await screen.findByRole('heading', { name: 'Reviews for Java Basics' })).toBeInTheDocument();
  });

  it('routes /login to the sign-in page', () => {
    mockApi();

    renderApp('/login?redirect=%2Fcatalog');

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('redirects unknown paths to the catalog and keeps the query string', async () => {
    const calls = mockApi();

    renderApp('/does-not-exist?search=java');

    await waitFor(() => expect(calls.some((url) => url.startsWith('/api/courses?'))).toBe(true));
    const catalogCall = calls.find((url) => url.startsWith('/api/courses?'))!;
    expect(new URL(catalogCall, 'http://localhost').searchParams.get('search')).toBe('java');
    expect(screen.getByRole('search')).toBeInTheDocument();
  });
});
