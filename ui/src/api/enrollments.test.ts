import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  enrollInCourse,
  enrollmentStatus,
  fetchCurrentEnrollment,
  fetchEnrollment,
  fetchMyEnrollments,
  setEnrollmentArchived,
  updateLectureProgress,
} from './enrollments';
import { setToken } from './http';

const COURSE_UUID = '123e4567-e89b-12d3-a456-426655440001';
const ENROLLMENT_UUID = '323e4567-e89b-12d3-a456-426655440001';
const LECTURE_UUID = '223e4567-e89b-12d3-a456-426655440001';

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
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
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

const enrollment = {
  uuid: ENROLLMENT_UUID,
  course: COURSE_UUID,
  courseName: 'Java Basics',
  student: 'alice',
  completionStatus: 'IN_PROGRESS' as const,
  archived: false,
  completedLectures: 1,
  totalLectures: 2,
  progressPercent: 50,
  enrolledAt: '2026-10-01T10:00:00',
  lastActivityAt: '2026-10-02T10:00:00',
  completedAt: null,
};

describe('enrollments api', () => {
  beforeEach(() => {
    localStorage.clear();
    setToken(STUDENT_TOKEN);
  });
  afterEach(() => vi.restoreAllMocks());

  it('fetchMyEnrollments requests the first page without filters by default', async () => {
    const page = { items: [enrollment], page: 0, size: 12, totalElements: 1, totalPages: 1, counts: { inProgress: 1, completed: 0, archived: 0 } };
    const calls = mockFetch(200, page);

    await expect(fetchMyEnrollments()).resolves.toEqual(page);

    expect(calls[0].url).toBe('/api/course-enrollments');
    expect(headers(calls[0].init).Authorization).toBe(`Bearer ${STUDENT_TOKEN}`);
  });

  it('fetchMyEnrollments passes status and page as query parameters', async () => {
    const calls = mockFetch(200, {});

    await fetchMyEnrollments({ status: 'ARCHIVED', page: 2, size: 5 });

    expect(calls[0].url).toBe('/api/course-enrollments?status=ARCHIVED&page=2&size=5');
  });

  it('fetchEnrollment GETs the enrollment details by uuid', async () => {
    const details = { enrollment, lectures: [] };
    const calls = mockFetch(200, details);

    await expect(fetchEnrollment(ENROLLMENT_UUID)).resolves.toEqual(details);

    expect(calls[0].url).toBe(`/api/course-enrollments/${ENROLLMENT_UUID}`);
  });

  it('fetchCurrentEnrollment resolves to null when the student is not enrolled', async () => {
    const calls = mockFetch(404, { message: 'not found' });

    await expect(fetchCurrentEnrollment(COURSE_UUID)).resolves.toBeNull();

    expect(calls[0].url).toBe(`/api/courses/${COURSE_UUID}/course-enrollments/current`);
  });

  it('fetchCurrentEnrollment rethrows non-404 failures', async () => {
    mockFetch(500, {});

    await expect(fetchCurrentEnrollment(COURSE_UUID)).rejects.toMatchObject({ status: 500 });
  });

  it('enrollInCourse POSTs the student as JSON and returns the created enrollment id', async () => {
    const calls = mockFetch(201, 'enrollment-uuid');

    await expect(enrollInCourse(COURSE_UUID, 'alice')).resolves.toBe('enrollment-uuid');

    expect(calls[0].url).toBe(`/api/courses/${COURSE_UUID}/course-enrollments`);
    expect(calls[0].init?.method).toBe('POST');
    expect(JSON.parse(String(calls[0].init?.body))).toEqual({ student: 'alice' });
    expect(headers(calls[0].init)).toMatchObject({
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${STUDENT_TOKEN}`,
    });
  });

  it('enrollInCourse rejects with the forbidden status for anonymous callers', async () => {
    localStorage.clear();
    const calls = mockFetch(403, { message: 'forbidden' });

    await expect(enrollInCourse(COURSE_UUID, 'alice')).rejects.toMatchObject({ status: 403 });

    expect(headers(calls[0].init).Authorization).toBeUndefined();
  });

  it('updateLectureProgress PUTs the completed flag for the lecture', async () => {
    const calls = mockFetch(200, { enrollment, lectures: [] });

    await updateLectureProgress(ENROLLMENT_UUID, LECTURE_UUID, true);

    expect(calls[0].url).toBe(`/api/course-enrollments/${ENROLLMENT_UUID}/lectures/${LECTURE_UUID}/progress`);
    expect(calls[0].init?.method).toBe('PUT');
    expect(JSON.parse(String(calls[0].init?.body))).toEqual({ completed: true });
  });

  it('setEnrollmentArchived PUTs the archive status', async () => {
    const calls = mockFetch(200, { ...enrollment, archived: true });

    await expect(setEnrollmentArchived(ENROLLMENT_UUID, true)).resolves.toMatchObject({ archived: true });

    expect(calls[0].url).toBe(`/api/course-enrollments/${ENROLLMENT_UUID}/archive-status`);
    expect(calls[0].init?.method).toBe('PUT');
    expect(JSON.parse(String(calls[0].init?.body))).toEqual({ archived: true });
  });

  it('enrollmentStatus treats archived as its own status regardless of completion', () => {
    expect(enrollmentStatus(enrollment)).toBe('IN_PROGRESS');
    expect(enrollmentStatus({ ...enrollment, completionStatus: 'COMPLETED' })).toBe('COMPLETED');
    expect(enrollmentStatus({ ...enrollment, completionStatus: 'COMPLETED', archived: true })).toBe('ARCHIVED');
  });
});
