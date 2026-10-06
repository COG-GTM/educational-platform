import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  enrollInCourse,
  fetchCourseDetails,
  fetchCourseReviews,
  fetchCourseReviewSummary,
  fetchMyEnrollments,
} from './courses';
import { HttpError, setToken } from './http';

const COURSE_UUID = '123e4567-e89b-12d3-a456-426655440001';

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

describe('courses api', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('fetchCourseDetails GETs the course by uuid and returns the parsed details', async () => {
    const details = { uuid: COURSE_UUID, name: 'Java Basics', curriculumItems: [] };
    const calls = mockFetch(200, details);

    await expect(fetchCourseDetails(COURSE_UUID)).resolves.toEqual(details);

    expect(calls).toHaveLength(1);
    expect(calls[0].url).toBe(`/api/courses/${COURSE_UUID}`);
    expect(calls[0].init?.method).toBe('GET');
    expect(calls[0].init?.body).toBeUndefined();
    expect(headers(calls[0].init)).toEqual({ Accept: 'application/json' });
  });

  it('fetchCourseDetails surfaces a 404 as an HttpError with the status', async () => {
    mockFetch(404, { message: 'not found' });

    await expect(fetchCourseDetails(COURSE_UUID)).rejects.toMatchObject({ name: 'HttpError', status: 404 });
    await expect(fetchCourseDetails(COURSE_UUID)).rejects.toBeInstanceOf(HttpError);
  });

  it('fetchCourseReviews and fetchCourseReviewSummary target the nested review resources', async () => {
    const calls = mockFetch(200, []);

    await fetchCourseReviews(COURSE_UUID);
    await fetchCourseReviewSummary(COURSE_UUID);

    expect(calls.map((c) => c.url)).toEqual([
      `/api/courses/${COURSE_UUID}/reviews`,
      `/api/courses/${COURSE_UUID}/reviews/summary`,
    ]);
    expect(calls.every((c) => c.init?.method === 'GET')).toBe(true);
  });

  it('escapes the course uuid in the path so a crafted id cannot change the resource', async () => {
    const calls = mockFetch(200, []);

    await fetchCourseReviews('../catalog-facets?x=1');

    expect(calls[0].url).toBe('/api/courses/..%2Fcatalog-facets%3Fx%3D1/reviews');
  });

  it('fetchMyEnrollments sends the stored bearer token to the enrollments endpoint', async () => {
    setToken(STUDENT_TOKEN);
    const enrollments = [{ uuid: 'e-1', course: COURSE_UUID, student: 'alice', completionStatus: 'IN_PROGRESS' }];
    const calls = mockFetch(200, enrollments);

    await expect(fetchMyEnrollments()).resolves.toEqual(enrollments);

    expect(calls[0].url).toBe('/api/course-enrollments');
    expect(headers(calls[0].init).Authorization).toBe(`Bearer ${STUDENT_TOKEN}`);
  });

  it('enrollInCourse POSTs the student as JSON and returns the created enrollment id', async () => {
    setToken(STUDENT_TOKEN);
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
    const calls = mockFetch(403, { message: 'forbidden' });

    await expect(enrollInCourse(COURSE_UUID, 'alice')).rejects.toMatchObject({ status: 403 });

    expect(headers(calls[0].init).Authorization).toBeUndefined();
  });
});
