import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { CourseDetails, CourseReview, CourseReviewSummary } from '../api/courses';
import { setToken } from '../api/http';
import { AuthProvider } from '../auth/AuthContext';
import CourseDetailPage from './CourseDetailPage';

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

const course: CourseDetails = {
  uuid: COURSE_UUID,
  name: 'Java Basics',
  description: 'Learn Java from scratch',
  category: 'Programming',
  teacherName: 'teacher-bob',
  rating: 4.5,
  numberOfStudents: 3,
  publishedDate: '2026-09-01T10:00:00',
  curriculumItems: [
    { uuid: 'ci-1', title: 'Intro lecture', description: 'Welcome', serialNumber: 1, type: 'LECTURE' },
    { uuid: 'ci-2', title: 'Intro quiz', description: null, serialNumber: 2, type: 'QUIZ' },
  ],
};

const reviews: CourseReview[] = [
  { uuid: 'r-1', course: COURSE_UUID, username: 'carol', comment: 'Great course', rating: 5, createdDate: '2026-09-03T10:00:00' },
  { uuid: 'r-2', course: COURSE_UUID, username: 'dave', comment: 'Good', rating: 4, createdDate: '2026-09-02T10:00:00' },
  { uuid: 'r-3', course: COURSE_UUID, username: 'erin', comment: null, rating: 4, createdDate: '2026-09-01T10:00:00' },
  { uuid: 'r-4', course: COURSE_UUID, username: 'frank', comment: 'Old one', rating: 3, createdDate: '2026-08-01T10:00:00' },
];

const summary: CourseReviewSummary = {
  averageRating: 4.0,
  totalReviews: 4,
  ratingCounts: { '1': 0, '2': 0, '3': 1, '4': 2, '5': 1 },
};

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  } as Response;
}

interface MockOptions {
  courseStatus?: number;
  enrollments?: { course: string }[];
  enrollmentsStatus?: number;
  enrollStatus?: number;
}

function mockApi(options: MockOptions = {}) {
  const calls: { url: string; method: string; headers: Record<string, string> }[] = [];
  vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    calls.push({ url, method, headers: (init?.headers ?? {}) as Record<string, string> });
    if (url.endsWith('/reviews/summary')) return Promise.resolve(response(200, summary));
    if (url.endsWith('/reviews')) return Promise.resolve(response(200, reviews));
    if (url.endsWith('/course-enrollments') && method === 'POST') {
      return Promise.resolve(response(options.enrollStatus ?? 201, 'enrollment-uuid'));
    }
    if (url.endsWith('/course-enrollments')) {
      const status = options.enrollmentsStatus ?? 200;
      return Promise.resolve(response(status, status === 200 ? (options.enrollments ?? []) : {}));
    }
    if (url.endsWith(`/api/courses/${COURSE_UUID}`)) {
      const status = options.courseStatus ?? 200;
      return Promise.resolve(response(status, status === 200 ? course : { message: 'not found' }));
    }
    return Promise.resolve(response(404));
  });
  return calls;
}

function renderPage(path = `/courses/${COURSE_UUID}`) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/courses/:uuid" element={<CourseDetailPage />} />
          <Route path="/login" element={<h2>Sign in</h2>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('CourseDetailPage', () => {
  beforeEach(() => setToken(null));
  afterEach(() => vi.restoreAllMocks());

  it('renders header, description, ordered curriculum and recent reviews from the backend', async () => {
    mockApi();
    renderPage();

    expect(await screen.findByRole('heading', { level: 2, name: 'Java Basics' })).toBeInTheDocument();
    expect(screen.getByText('teacher-bob')).toBeInTheDocument();
    expect(screen.getByLabelText('Rated 4.5 out of 5')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('Learn Java from scratch')).toBeInTheDocument();

    const curriculum = within(screen.getByRole('region', { name: 'Curriculum' })).getAllByRole('listitem');
    expect(curriculum).toHaveLength(2);
    expect(curriculum[0]).toHaveTextContent('Lecture');
    expect(curriculum[0]).toHaveTextContent('Intro lecture');
    expect(curriculum[1]).toHaveTextContent('Quiz');
    expect(curriculum[1]).toHaveTextContent('Intro quiz');

    const reviewsSection = screen.getByRole('region', { name: 'Reviews' });
    expect(within(reviewsSection).getByText('4 reviews')).toBeInTheDocument();
    expect(within(reviewsSection).getByLabelText('2 reviews with 4 stars')).toHaveAttribute('aria-valuenow', '50');
    expect(within(reviewsSection).getByText('carol')).toBeInTheDocument();
    expect(within(reviewsSection).getByText('erin')).toBeInTheDocument();
    expect(within(reviewsSection).queryByText('frank')).not.toBeInTheDocument();
    expect(within(reviewsSection).getByRole('link', { name: 'See all 4 reviews' })).toHaveAttribute(
      'href',
      `/courses/${COURSE_UUID}/reviews`,
    );
  });

  it('prompts anonymous visitors to sign in, preserving the course URL', async () => {
    mockApi();
    renderPage();

    const link = await screen.findByRole('link', { name: 'Sign in to enroll' });
    expect(link).toHaveAttribute('href', `/login?redirect=${encodeURIComponent(`/courses/${COURSE_UUID}`)}`);
    expect(screen.queryByRole('button', { name: 'Enroll' })).not.toBeInTheDocument();
  });

  it('shows the enrolled state for a student who is already enrolled', async () => {
    setToken(STUDENT_TOKEN);
    mockApi({ enrollments: [{ course: COURSE_UUID }] });
    renderPage();

    expect(await screen.findByRole('link', { name: 'Enrolled — go to course' })).toHaveAttribute('href', '#curriculum');
    expect(screen.queryByRole('button', { name: 'Enroll' })).not.toBeInTheDocument();
  });

  it('enrolls a student and updates the button and student count without reloading', async () => {
    setToken(STUDENT_TOKEN);
    const calls = mockApi({ enrollments: [] });
    renderPage();

    const enroll = await screen.findByRole('button', { name: 'Enroll' });
    fireEvent.click(enroll);

    expect(await screen.findByRole('link', { name: 'Enrolled — go to course' })).toBeInTheDocument();
    expect(screen.getByRole('status', { name: '' })).toHaveTextContent("You're enrolled!");
    expect(screen.getByText('4')).toBeInTheDocument();

    const enrollCall = calls.find((c) => c.method === 'POST');
    expect(enrollCall?.url).toBe(`/api/courses/${COURSE_UUID}/course-enrollments`);
    expect(enrollCall?.headers.Authorization).toBe(`Bearer ${STUDENT_TOKEN}`);
    expect(fetch).toHaveBeenCalledTimes(5);
  });

  it('keeps Enroll disabled with a retry when the enrollment check fails', async () => {
    setToken(STUDENT_TOKEN);
    mockApi({ enrollmentsStatus: 500 });
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't check your enrollment");
    expect(screen.getByRole('button', { name: 'Enroll' })).toBeDisabled();

    vi.restoreAllMocks();
    mockApi({ enrollments: [{ course: COURSE_UUID }] });
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('link', { name: 'Enrolled — go to course' })).toBeInTheDocument();
  });

  it('does not send an expired token on public requests', async () => {
    setToken(makeToken({ sub: 'alice', auth: [{ authority: 'ROLE_STUDENT' }], exp: Math.floor(Date.now() / 1000) - 60 }));
    const calls = mockApi();
    renderPage();

    await screen.findByRole('link', { name: 'Sign in to enroll' });
    expect(calls.every((c) => c.headers.Authorization === undefined)).toBe(true);
    expect(localStorage.getItem('educational-platform.token')).toBeNull();
  });

  it('shows an error and keeps the Enroll button when enrollment fails', async () => {
    setToken(STUDENT_TOKEN);
    mockApi({ enrollments: [], enrollStatus: 500 });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Enroll' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Enrollment failed');
    expect(screen.getByRole('button', { name: 'Enroll' })).toBeEnabled();
  });

  it('shows a friendly not-found message for unpublished or removed courses', async () => {
    mockApi({ courseStatus: 404 });
    renderPage();

    expect(await screen.findByRole('heading', { name: "This course isn't available" })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to catalog' })).toHaveAttribute('href', '/catalog');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows a retryable error when the backend fails', async () => {
    mockApi({ courseStatus: 500 });
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't load this course");
    await waitFor(() => expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument());
  });
});
