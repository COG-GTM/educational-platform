import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CourseEnrollment } from '../api/enrollments';
import { setToken } from '../api/http';
import { AuthProvider } from '../auth/AuthContext';
import MyLearningPage from './MyLearningPage';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const STUDENT_TOKEN = makeToken({ sub: 'alice', auth: [{ authority: 'ROLE_STUDENT' }], exp: Math.floor(Date.now() / 1000) + 3600 });
const TEACHER_TOKEN = makeToken({ sub: 'bob', auth: [{ authority: 'ROLE_TEACHER' }], exp: Math.floor(Date.now() / 1000) + 3600 });

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as Response;
}

function enrollment(overrides: Partial<CourseEnrollment> = {}): CourseEnrollment {
  return {
    uuid: 'e-1',
    course: 'c-1',
    courseName: 'Java Basics',
    student: 'alice',
    completionStatus: 'IN_PROGRESS',
    archived: false,
    completedLectures: 1,
    totalLectures: 4,
    progressPercent: 25,
    enrolledAt: '2026-10-01T10:00:00',
    lastActivityAt: '2026-10-02T10:00:00',
    completedAt: null,
    ...overrides,
  };
}

interface MockOptions {
  pages?: Record<string, CourseEnrollment[]>;
  counts?: { inProgress: number; completed: number; archived: number };
  listStatus?: number;
  archiveStatus?: number;
  totalPages?: number;
}

function mockApi(options: MockOptions = {}) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    calls.push({ url, method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    if (url.endsWith('/archive-status')) {
      return Promise.resolve(response(options.archiveStatus ?? 200, enrollment({ archived: true })));
    }
    if (url.startsWith('/api/course-enrollments')) {
      if (options.listStatus && options.listStatus !== 200) return Promise.resolve(response(options.listStatus, {}));
      const params = new URL(url, 'http://localhost').searchParams;
      const status = params.get('status') ?? 'IN_PROGRESS';
      const items = options.pages?.[status] ?? [];
      const counts = options.counts ?? { inProgress: 0, completed: 0, archived: 0 };
      return Promise.resolve(
        response(200, {
          items,
          page: Number(params.get('page') ?? '0'),
          size: 12,
          totalElements: items.length,
          totalPages: options.totalPages ?? 1,
          counts,
        }),
      );
    }
    return Promise.resolve(response(404));
  });
  return calls;
}

/** Serves one enrollment per page that exists and an empty page beyond `totalPages`, like the backend does. */
function mockPagedApi({ totalElements, totalPages }: { totalElements: number; totalPages: number }) {
  const calls: string[] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = String(input);
    calls.push(url);
    const params = new URL(url, 'http://localhost').searchParams;
    const page = Number(params.get('page') ?? '0');
    return Promise.resolve(
      response(200, {
        items: page < totalPages ? [enrollment()] : [],
        page,
        size: 12,
        totalElements,
        totalPages,
        counts: { inProgress: totalElements, completed: 0, archived: 0 },
      }),
    );
  });
  return calls;
}

function renderPage(path = '/learning') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/learning" element={<MyLearningPage />} />
          <Route path="/learning/:uuid" element={<h2>Course progress</h2>} />
          <Route path="/catalog" element={<h2>Catalog</h2>} />
          <Route path="/login" element={<h2>Sign in</h2>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('MyLearningPage', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('asks anonymous visitors to sign in and does not call the API', () => {
    const calls = mockApi();
    renderPage();

    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login?redirect=%2Flearning');
    expect(calls).toHaveLength(0);
  });

  it('tells non-students the dashboard is for students only', () => {
    setToken(TEACHER_TOKEN);
    mockApi();
    renderPage();

    expect(screen.getByText('Only students have a learning dashboard.')).toBeInTheDocument();
  });

  it('shows the empty state linking to the catalog when the student has no enrollments', async () => {
    setToken(STUDENT_TOKEN);
    mockApi();
    renderPage();

    expect(await screen.findByRole('heading', { name: "You haven't enrolled in any courses yet" })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Explore the catalog' })).toHaveAttribute('href', '/catalog');
    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
  });

  it('lists in-progress enrollments with progress, last activity and tab counts', async () => {
    setToken(STUDENT_TOKEN);
    mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 1, completed: 2, archived: 3 } });
    renderPage();

    expect(await screen.findByRole('link', { name: 'Java Basics' })).toHaveAttribute('href', '/learning/e-1');
    expect(screen.getByRole('progressbar', { name: 'Java Basics progress' })).toHaveAttribute('aria-valuenow', '25');
    expect(screen.getByText('25% · 1 of 4 lectures completed')).toBeInTheDocument();
    expect(screen.getByText(/Last activity/)).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'In progress 1' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Completed 2' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Archived 3' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Continue learning' })).toHaveAttribute('href', '/learning/e-1');
  });

  it('switches tabs through the status query parameter', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({
      pages: {
        IN_PROGRESS: [enrollment()],
        COMPLETED: [enrollment({ uuid: 'e-2', courseName: 'Spring Boot', completionStatus: 'COMPLETED', progressPercent: 100, completedAt: '2026-10-03T10:00:00' })],
      },
      counts: { inProgress: 1, completed: 1, archived: 0 },
    });
    renderPage();

    await screen.findByRole('link', { name: 'Java Basics' });
    fireEvent.click(screen.getByRole('tab', { name: 'Completed 1' }));

    expect(await screen.findByRole('link', { name: 'Spring Boot' })).toBeInTheDocument();
    expect(screen.getByText(/Completed on/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Review course' })).toBeInTheDocument();
    expect(calls[calls.length - 1]?.url).toBe('/api/course-enrollments?status=COMPLETED');
  });

  it('shows a tab-specific empty message when only that status is empty', async () => {
    setToken(STUDENT_TOKEN);
    mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 1, completed: 0, archived: 0 } });
    renderPage('/learning?status=ARCHIVED');

    expect(await screen.findByText(/No archived courses/)).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Archived 0' })).toHaveAttribute('aria-selected', 'true');
  });

  it('archives an enrollment, confirms it and offers undo', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 1, completed: 0, archived: 0 } });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Archive' }));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('"Java Basics" was archived'));
    const archiveCall = calls.find((c) => c.method === 'PUT');
    expect(archiveCall?.url).toBe('/api/course-enrollments/e-1/archive-status');
    expect(archiveCall?.body).toEqual({ archived: true });

    fireEvent.click(screen.getByRole('button', { name: 'Undo' }));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('"Java Basics" is back in your active list.'));
    expect(calls.filter((c) => c.method === 'PUT').pop()?.body).toEqual({ archived: false });
  });

  it('restores from the archived tab', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({ pages: { ARCHIVED: [enrollment({ archived: true })] }, counts: { inProgress: 0, completed: 0, archived: 1 } });
    renderPage('/learning?status=ARCHIVED');

    fireEvent.click(await screen.findByRole('button', { name: 'Restore' }));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('"Java Basics" was restored'));
    expect(calls.find((c) => c.method === 'PUT')?.body).toEqual({ archived: false });
  });

  it('shows an error when archiving fails', async () => {
    setToken(STUDENT_TOKEN);
    mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 1, completed: 0, archived: 0 }, archiveStatus: 500 });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Archive' }));

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't archive \"Java Basics\"");
    expect(screen.getByRole('button', { name: 'Archive' })).toBeEnabled();
  });

  it('shows an error with retry when loading fails', async () => {
    setToken(STUDENT_TOKEN);
    mockApi({ listStatus: 500 });
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't load your courses");

    vi.restoreAllMocks();
    mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 1, completed: 0, archived: 0 } });
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('link', { name: 'Java Basics' })).toBeInTheDocument();
  });

  it('renders pagination for students with many enrollments', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 30, completed: 0, archived: 0 }, totalPages: 3 });
    renderPage();

    await screen.findByRole('link', { name: 'Java Basics' });
    fireEvent.click(screen.getByRole('button', { name: /next/i }));

    await waitFor(() => expect(calls[calls.length - 1]?.url).toBe('/api/course-enrollments?status=IN_PROGRESS&page=1'));
  });

  it('falls back to the In progress tab when the status parameter is not a known group', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 1, completed: 0, archived: 0 } });
    renderPage('/learning?status=BOGUS');

    expect(await screen.findByRole('link', { name: 'Java Basics' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'In progress 1' })).toHaveAttribute('aria-selected', 'true');
    expect(calls.map((c) => c.url)).toEqual(['/api/course-enrollments?status=IN_PROGRESS']);
  });

  it.each(['-3', 'abc'])('treats page=%s as the first page', async (page) => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({ pages: { IN_PROGRESS: [enrollment()] }, counts: { inProgress: 1, completed: 0, archived: 0 } });
    renderPage(`/learning?page=${page}`);

    expect(await screen.findByRole('link', { name: 'Java Basics' })).toBeInTheDocument();
    expect(calls.map((c) => c.url)).toEqual(['/api/course-enrollments?status=IN_PROGRESS']);
  });

  it('snaps back to the last page when the requested page is beyond the results', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockPagedApi({ totalElements: 13, totalPages: 2 });
    renderPage('/learning?page=5');

    expect(await screen.findByRole('link', { name: 'Java Basics' })).toBeInTheDocument();
    expect(calls).toEqual([
      '/api/course-enrollments?status=IN_PROGRESS&page=5',
      '/api/course-enrollments?status=IN_PROGRESS&page=1',
    ]);
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();
  });

  it('drops the page parameter when the only page left is the first one', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockPagedApi({ totalElements: 3, totalPages: 1 });
    renderPage('/learning?page=2');

    expect(await screen.findByRole('link', { name: 'Java Basics' })).toBeInTheDocument();
    expect(calls).toEqual(['/api/course-enrollments?status=IN_PROGRESS&page=2', '/api/course-enrollments?status=IN_PROGRESS']);
    expect(screen.queryByRole('navigation', { name: 'Enrollment pages' })).not.toBeInTheDocument();
  });

  it('switching tabs starts from the first page of the new group', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({
      pages: { IN_PROGRESS: [enrollment()], COMPLETED: [enrollment({ uuid: 'e-2', courseName: 'Spring Boot', completionStatus: 'COMPLETED' })] },
      counts: { inProgress: 30, completed: 1, archived: 0 },
      totalPages: 3,
    });
    renderPage('/learning?page=2');

    await screen.findByRole('link', { name: 'Java Basics' });
    expect(calls[0]?.url).toBe('/api/course-enrollments?status=IN_PROGRESS&page=2');
    fireEvent.click(screen.getByRole('tab', { name: 'Completed 1' }));

    expect(await screen.findByRole('link', { name: 'Spring Boot' })).toBeInTheDocument();
    expect(calls[calls.length - 1]?.url).toBe('/api/course-enrollments?status=COMPLETED');
  });
});
