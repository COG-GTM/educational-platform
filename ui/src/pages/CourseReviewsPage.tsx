import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  CourseDetails,
  CourseReview,
  CourseReviewSummary,
  fetchCourseDetails,
  fetchCourseReviews,
  fetchCourseReviewSummary,
} from '../api/courses';
import { HttpError } from '../api/http';
import RatingBreakdown from '../components/RatingBreakdown';
import ReviewCard from '../components/ReviewCard';

type LoadState = 'loading' | 'ready' | 'not-found' | 'error';

export default function CourseReviewsPage() {
  const { uuid = '' } = useParams();
  const [state, setState] = useState<LoadState>('loading');
  const [course, setCourse] = useState<CourseDetails | null>(null);
  const [reviews, setReviews] = useState<CourseReview[]>([]);
  const [summary, setSummary] = useState<CourseReviewSummary | null>(null);

  useEffect(() => {
    let cancelled = false;
    setState('loading');
    Promise.all([fetchCourseDetails(uuid), fetchCourseReviews(uuid), fetchCourseReviewSummary(uuid)])
      .then(([details, reviewList, reviewSummary]) => {
        if (cancelled) return;
        setCourse(details);
        setReviews(reviewList);
        setSummary(reviewSummary);
        setState('ready');
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setState(e instanceof HttpError && e.status === 404 ? 'not-found' : 'error');
      });
    return () => {
      cancelled = true;
    };
  }, [uuid]);

  if (state === 'loading') {
    return (
      <div className="catalog-state" role="status">
        <div className="spinner" aria-hidden="true" />
        <p>Loading reviews…</p>
      </div>
    );
  }

  if (state === 'not-found') {
    return (
      <section className="catalog-state course-not-found">
        <h2>This course isn't available</h2>
        <p>It may have been unpublished or removed.</p>
        <Link to="/catalog" className="button-link">
          Back to catalog
        </Link>
      </section>
    );
  }

  if (state === 'error' || !course) {
    return (
      <div className="catalog-state catalog-error" role="alert">
        <p>We couldn't load the reviews right now.</p>
        <Link to={`/courses/${uuid}`}>Back to course</Link>
      </div>
    );
  }

  return (
    <section className="course-detail" aria-labelledby="reviews-title">
      <nav aria-label="Breadcrumb" className="breadcrumb">
        <Link to="/catalog">Catalog</Link> <span aria-hidden="true">/</span>{' '}
        <Link to={`/courses/${course.uuid}`}>{course.name}</Link> <span aria-hidden="true">/</span>{' '}
        <span>Reviews</span>
      </nav>
      <h2 id="reviews-title">Reviews for {course.name}</h2>
      {summary && <RatingBreakdown summary={summary} />}
      {reviews.length === 0 ? (
        <p className="course-detail-empty">No reviews yet.</p>
      ) : (
        <ul className="review-list">
          {reviews.map((review) => (
            <ReviewCard key={review.uuid} review={review} />
          ))}
        </ul>
      )}
    </section>
  );
}
