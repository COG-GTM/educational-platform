import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { CourseDetails, CourseReview, CourseReviewSummary } from '../api/courses';
import CourseReviewsPage from './CourseReviewsPage';

const COURSE_UUID = '123e4567-e89b-12d3-a456-426655440002';

const course: CourseDetails = {
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

const reviews: CourseReview[] = [
  { uuid: 'r-1', course: COURSE_UUID, username: 'carol', comment: 'Great course', rating: 5, createdDate: '2026-09-03T10:00:00' },
  { uuid: 'r-2', course: COURSE_UUID, username: 'dave', comment: null, rating: 4, createdDate: null },
];

const summary: CourseReviewSummary = {
  averageRating: 4.5,
  totalReviews: 2,
  ratingCounts: { '1': 0, '2': 0, '3': 0, '4': 1, '5': 1 },
};

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as Response;
}

interface MockOptions {
  courseStatus?: number;
  reviews?: CourseReview[];
  reviewsStatus?: number;
  summaryStatus?: number;
}

function mockApi(options: MockOptions = {}) {
  vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = String(input);
    if (url.endsWith('/reviews/summary')) {
      const status = options.summaryStatus ?? 200;
      return Promise.resolve(response(status, status === 200 ? summary : {}));
    }
    if (url.endsWith('/reviews')) {
      const status = options.reviewsStatus ?? 200;
      return Promise.resolve(response(status, status === 200 ? (options.reviews ?? reviews) : {}));
    }
    if (url.endsWith(`/api/courses/${COURSE_UUID}`)) {
      const status = options.courseStatus ?? 200;
      return Promise.resolve(response(status, status === 200 ? course : { message: 'not found' }));
    }
    return Promise.resolve(response(404));
  });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/courses/${COURSE_UUID}/reviews`]}>
      <Routes>
        <Route path="/courses/:uuid/reviews" element={<CourseReviewsPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CourseReviewsPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows a loading state before the data arrives', () => {
    mockApi();
    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('Loading reviews…');
  });

  it('renders the breadcrumb, rating breakdown and every review in backend order', async () => {
    mockApi();
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Reviews for Java Basics' })).toBeInTheDocument();
    const breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' });
    expect(within(breadcrumb).getByRole('link', { name: 'Catalog' })).toHaveAttribute('href', '/catalog');
    expect(within(breadcrumb).getByRole('link', { name: 'Java Basics' })).toHaveAttribute('href', `/courses/${COURSE_UUID}`);

    expect(screen.getByRole('list', { name: 'Rating breakdown' })).toBeInTheDocument();
    expect(screen.getByText('2 reviews')).toBeInTheDocument();

    const cards = document.querySelectorAll('.review-card');
    expect(cards).toHaveLength(2);
    expect(cards[0]).toHaveTextContent('carol');
    expect(cards[0]).toHaveTextContent('Great course');
    expect(cards[1]).toHaveTextContent('dave');
    expect(cards[1]).toHaveTextContent('No written comment.');
  });

  it('still renders the reviews when the summary request fails', async () => {
    mockApi({ summaryStatus: 500 });
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Reviews for Java Basics' })).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'Rating breakdown' })).not.toBeInTheDocument();
    expect(document.querySelectorAll('.review-card')).toHaveLength(2);
  });

  it('shows an empty message when the course has no reviews', async () => {
    mockApi({ reviews: [] });
    renderPage();

    expect(await screen.findByText('No reviews yet.')).toBeInTheDocument();
    expect(document.querySelectorAll('.review-card')).toHaveLength(0);
  });

  it('shows a friendly not-found page when the course is not published', async () => {
    mockApi({ courseStatus: 404 });
    renderPage();

    expect(await screen.findByRole('heading', { name: "This course isn't available" })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to catalog' })).toHaveAttribute('href', '/catalog');
  });

  it('shows an error with a link back to the course when loading reviews fails', async () => {
    mockApi({ reviewsStatus: 500 });
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't load the reviews right now.");
    expect(screen.getByRole('link', { name: 'Back to course' })).toHaveAttribute('href', `/courses/${COURSE_UUID}`);
  });
});
