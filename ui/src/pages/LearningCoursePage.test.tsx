import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CourseEnrollmentDetails } from '../api/enrollments';
import { setToken } from '../api/http';
import { AuthProvider } from '../auth/AuthContext';
import LearningCoursePage from './LearningCoursePage';

function makeToken(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const STUDENT_TOKEN = makeToken({ sub: 'alice', auth: [{ authority: 'ROLE_STUDENT' }], exp: Math.floor(Date.now() / 1000) + 3600 });

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as Response;
}

function details(completed: string[] = [], archived = false): CourseEnrollmentDetails {
  const lectures = [
    { uuid: 'l-1', title: 'Intro', serialNumber: 1, completed: completed.includes('l-1') },
    { uuid: 'l-2', title: 'Variables', serialNumber: 2, completed: completed.includes('l-2') },
  ];
  const done = lectures.filter((l) => l.completed).length;
  const isComplete = done === lectures.length;
  return {
    enrollment: {
      uuid: 'e-1',
      course: 'c-1',
      courseName: 'Java Basics',
      student: 'alice',
      completionStatus: isComplete ? 'COMPLETED' : 'IN_PROGRESS',
      archived,
      completedLectures: done,
      totalLectures: lectures.length,
      progressPercent: Math.round((done * 100) / lectures.length),
      enrolledAt: '2026-10-01T10:00:00',
      lastActivityAt: '2026-10-02T10:00:00',
      completedAt: isComplete ? '2026-10-03T10:00:00' : null,
    },
    lectures,
  };
}

interface MockOptions {
  initial?: CourseEnrollmentDetails;
  detailsStatus?: number;
  progressStatus?: number;
}

function mockApi(options: MockOptions = {}) {
  const calls: { url: string; method: string; body?: unknown }[] = [];
  let completed = (options.initial ?? details()).lectures.filter((l) => l.completed).map((l) => l.uuid);
  let archived = (options.initial ?? details()).enrollment.archived;
  vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;
    calls.push({ url, method, body });
    if (url.includes('/lectures/') && url.endsWith('/progress')) {
      if (options.progressStatus && options.progressStatus !== 200) return Promise.resolve(response(options.progressStatus, {}));
      const lecture = url.split('/lectures/')[1].split('/')[0];
      completed = body.completed ? [...new Set([...completed, lecture])] : completed.filter((l) => l !== lecture);
      return Promise.resolve(response(200, details(completed, archived)));
    }
    if (url.endsWith('/archive-status')) {
      archived = body.archived;
      return Promise.resolve(response(200, details(completed, archived).enrollment));
    }
    if (url === '/api/course-enrollments/e-1') {
      const status = options.detailsStatus ?? 200;
      return Promise.resolve(response(status, status === 200 ? details(completed, archived) : { message: 'nope' }));
    }
    return Promise.resolve(response(404));
  });
  return calls;
}

function renderPage(path = '/learning/e-1') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/learning/:uuid" element={<LearningCoursePage />} />
          <Route path="/learning" element={<h2>My Learning</h2>} />
          <Route path="/courses/:uuid" element={<h2>Course details page</h2>} />
          <Route path="/login" element={<h2>Sign in</h2>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('LearningCoursePage', () => {
  beforeEach(() => {
    localStorage.clear();
    setToken(STUDENT_TOKEN);
  });
  afterEach(() => vi.restoreAllMocks());

  it('asks anonymous visitors to sign in', () => {
    localStorage.clear();
    const calls = mockApi();
    renderPage();

    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login?redirect=%2Flearning%2Fe-1');
    expect(calls).toHaveLength(0);
  });

  it('renders the curriculum with completion checkboxes and progress', async () => {
    mockApi({ initial: details(['l-1']) });
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Java Basics' })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Course progress' })).toHaveAttribute('aria-valuenow', '50');
    expect(screen.getByTestId('progress-summary')).toHaveTextContent('50% · 1 of 2 lectures completed');
    expect(screen.getByRole('checkbox', { name: 'Mark "Intro" as not completed' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Mark "Variables" as completed' })).not.toBeChecked();
    expect(screen.getByRole('link', { name: 'Course details' })).toHaveAttribute('href', '/courses/c-1');
  });

  it('marks a lecture complete, updates progress immediately and confirms', async () => {
    const calls = mockApi();
    renderPage();

    fireEvent.click(await screen.findByRole('checkbox', { name: 'Mark "Intro" as completed' }));

    await waitFor(() => expect(screen.getByTestId('progress-summary')).toHaveTextContent('50% · 1 of 2 lectures completed'));
    expect(screen.getByRole('status')).toHaveTextContent('Marked "Intro" as completed.');
    const call = calls.find((c) => c.method === 'PUT');
    expect(call?.url).toBe('/api/course-enrollments/e-1/lectures/l-1/progress');
    expect(call?.body).toEqual({ completed: true });
  });

  it('celebrates when the last lecture is completed and allows reverting', async () => {
    mockApi({ initial: details(['l-1']) });
    renderPage();

    fireEvent.click(await screen.findByRole('checkbox', { name: 'Mark "Variables" as completed' }));

    expect(await screen.findByRole('heading', { name: /Congratulations — you completed Java Basics/ })).toBeInTheDocument();
    expect(screen.getByText('Completed', { selector: '.status-badge' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'See completed courses' })).toHaveAttribute('href', '/learning?status=COMPLETED');

    fireEvent.click(screen.getByRole('checkbox', { name: 'Mark "Variables" as not completed' }));

    await waitFor(() => expect(screen.getByTestId('progress-summary')).toHaveTextContent('50%'));
    expect(screen.queryByRole('heading', { name: /Congratulations/ })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('The course is back in progress.');
  });

  it('shows an error and keeps the checkbox state when saving fails', async () => {
    mockApi({ progressStatus: 500 });
    renderPage();

    fireEvent.click(await screen.findByRole('checkbox', { name: 'Mark "Intro" as completed' }));

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't save your progress");
    expect(screen.getByRole('checkbox', { name: 'Mark "Intro" as completed' })).not.toBeChecked();
  });

  it('locks lectures while archived and unlocks after restore', async () => {
    mockApi({ initial: details(['l-1'], true) });
    renderPage();

    expect(await screen.findByRole('note')).toHaveTextContent('This course is archived');
    expect(screen.getByRole('checkbox', { name: 'Mark "Variables" as completed' })).toBeDisabled();

    fireEvent.click(screen.getByRole('button', { name: 'Restore course' }));

    await waitFor(() => expect(screen.getByRole('checkbox', { name: 'Mark "Variables" as completed' })).toBeEnabled());
    expect(screen.getByRole('status')).toHaveTextContent('back in your active list');
    expect(screen.getByRole('button', { name: 'Archive course' })).toBeInTheDocument();
  });

  it('archives the course from the course page with confirmation', async () => {
    const calls = mockApi();
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Archive course' }));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('This course is archived and hidden from your active list'));
    expect(calls.find((c) => c.method === 'PUT')?.body).toEqual({ archived: true });
    expect(screen.getByRole('checkbox', { name: 'Mark "Intro" as completed' })).toBeDisabled();
  });

  it('shows a not-found state for unknown enrollments', async () => {
    mockApi({ detailsStatus: 404 });
    renderPage();

    expect(await screen.findByRole('heading', { name: "We couldn't find this enrollment" })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to My Learning' })).toHaveAttribute('href', '/learning');
  });
});
