import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import {
  CourseDetails,
  CourseReview,
  CourseReviewSummary,
  enrollInCourse,
  fetchCourseDetails,
  fetchCourseReviews,
  fetchCourseReviewSummary,
  fetchMyEnrollments,
} from '../api/courses';
import { HttpError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import RatingBreakdown from '../components/RatingBreakdown';
import ReviewCard from '../components/ReviewCard';
import StarRating from '../components/StarRating';
import { formatDate, formatDuration } from '../utils/format';

const RECENT_REVIEWS = 3;

type LoadState = 'loading' | 'ready' | 'not-found' | 'error';
type EnrollmentState = 'unknown' | 'checking' | 'check-failed' | 'not-enrolled' | 'enrolled';

export default function CourseDetailPage() {
  const { uuid = '' } = useParams();
  const location = useLocation();
  const { user, isStudent } = useAuth();

  const [state, setState] = useState<LoadState>('loading');
  const [course, setCourse] = useState<CourseDetails | null>(null);
  const [reviews, setReviews] = useState<CourseReview[]>([]);
  const [summary, setSummary] = useState<CourseReviewSummary | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  const [enrollment, setEnrollment] = useState<EnrollmentState>('unknown');
  const [enrollmentCheckToken, setEnrollmentCheckToken] = useState(0);
  const [enrolling, setEnrolling] = useState(false);
  const [enrollMessage, setEnrollMessage] = useState<string | null>(null);
  const [enrollError, setEnrollError] = useState<string | null>(null);
  const activeUuid = useRef(uuid);
  activeUuid.current = uuid;

  useEffect(() => {
    let cancelled = false;
    setState('loading');
    setCourse(null);
    setReviews([]);
    setSummary(null);
    setEnrollMessage(null);
    setEnrollError(null);
    setEnrolling(false);

    fetchCourseDetails(uuid)
      .then(async (details) => {
        const [reviewList, reviewSummary] = await Promise.all([
          fetchCourseReviews(uuid).catch(() => [] as CourseReview[]),
          fetchCourseReviewSummary(uuid).catch(() => null),
        ]);
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
  }, [uuid, reloadToken]);

  useEffect(() => {
    if (!user || !isStudent) {
      setEnrollment('unknown');
      return;
    }
    let cancelled = false;
    setEnrollment('checking');
    fetchMyEnrollments()
      .then((enrollments) => {
        if (cancelled) return;
        setEnrollment(enrollments.some((e) => e.course === uuid) ? 'enrolled' : 'not-enrolled');
      })
      .catch(() => {
        if (!cancelled) setEnrollment('check-failed');
      });
    return () => {
      cancelled = true;
    };
  }, [user, isStudent, uuid, enrollmentCheckToken]);

  const onEnroll = useCallback(async () => {
    if (!user) return;
    const target = uuid;
    setEnrolling(true);
    setEnrollError(null);
    try {
      await enrollInCourse(target, user.username);
      if (activeUuid.current !== target) return;
      setEnrollment('enrolled');
      setEnrollMessage("You're enrolled! The course is now in your learning list.");
      setCourse((current) =>
        current ? { ...current, numberOfStudents: current.numberOfStudents + 1 } : current,
      );
    } catch {
      if (activeUuid.current === target) setEnrollError('Enrollment failed. Please try again.');
    } finally {
      if (activeUuid.current === target) setEnrolling(false);
    }
  }, [uuid, user]);

  if (state === 'loading') {
    return (
      <div className="catalog-state" role="status">
        <div className="spinner" aria-hidden="true" />
        <p>Loading course…</p>
      </div>
    );
  }

  if (state === 'not-found') {
    return (
      <section className="catalog-state course-not-found">
        <h2>This course isn't available</h2>
        <p>It may have been unpublished or removed. Browse the catalog to find other courses.</p>
        <Link to="/catalog" className="button-link">
          Back to catalog
        </Link>
      </section>
    );
  }

  if (state === 'error' || !course) {
    return (
      <div className="catalog-state catalog-error" role="alert">
        <p>We couldn't load this course right now.</p>
        <button type="button" onClick={() => setReloadToken((token) => token + 1)}>
          Try again
        </button>
      </div>
    );
  }

  const loginHref = `/login?redirect=${encodeURIComponent(location.pathname)}`;
  const recentReviews = reviews.slice(0, RECENT_REVIEWS);
  const totalReviews = summary?.totalReviews ?? reviews.length;

  return (
    <article className="course-detail" aria-labelledby="course-title">
      <nav aria-label="Breadcrumb" className="breadcrumb">
        <Link to="/catalog">Catalog</Link> <span aria-hidden="true">/</span> <span>{course.name}</span>
      </nav>

      <header className="course-detail-header">
        <div className="course-detail-heading">
          <h2 id="course-title">{course.name}</h2>
          {course.category && <span className="course-card-category">{course.category}</span>}
        </div>
        <dl className="course-detail-meta">
          <div>
            <dt>Teacher</dt>
            <dd>{course.teacherName}</dd>
          </div>
          <div>
            <dt>Rating</dt>
            <dd>
              <StarRating rating={course.rating} />
            </dd>
          </div>
          <div>
            <dt>Students</dt>
            <dd>{course.numberOfStudents}</dd>
          </div>
          <div>
            <dt>Published</dt>
            <dd>
              {course.publishedDate ? (
                <time dateTime={course.publishedDate}>{formatDate(course.publishedDate)}</time>
              ) : (
                'Not available'
              )}
            </dd>
          </div>
        </dl>

        <div className="course-detail-cta">
          {!user && (
            <>
              <Link to={loginHref} className="button-link">
                Sign in to enroll
              </Link>
              <p className="course-detail-cta-hint">You need an account to enroll in this course.</p>
            </>
          )}
          {user && !isStudent && (
            <p className="course-detail-cta-hint">Only students can enroll in courses.</p>
          )}
          {user && isStudent && enrollment === 'checking' && (
            <button type="button" disabled>
              Checking enrollment…
            </button>
          )}
          {user && isStudent && enrollment === 'check-failed' && (
            <>
              <button type="button" disabled>
                Enroll
              </button>
              <p className="form-error" role="alert">
                We couldn't check your enrollment.{' '}
                <button
                  type="button"
                  className="link-button"
                  onClick={() => setEnrollmentCheckToken((token) => token + 1)}
                >
                  Retry
                </button>
              </p>
            </>
          )}
          {user && isStudent && enrollment === 'not-enrolled' && (
            <button type="button" onClick={onEnroll} disabled={enrolling}>
              {enrolling ? 'Enrolling…' : 'Enroll'}
            </button>
          )}
          {user && isStudent && enrollment === 'enrolled' && (
            <a href="#curriculum" className="button-link button-enrolled">
              Enrolled — go to course
            </a>
          )}
          <p className="course-detail-cta-status" role="status" aria-live="polite">
            {enrollMessage}
          </p>
          {enrollError && (
            <p className="form-error" role="alert">
              {enrollError}
            </p>
          )}
        </div>
      </header>

      <section className="course-detail-section" aria-labelledby="about-heading">
        <h3 id="about-heading">About this course</h3>
        <p className="course-detail-description">{course.description}</p>
      </section>

      <section className="course-detail-section" id="curriculum" aria-labelledby="curriculum-heading">
        <h3 id="curriculum-heading">Curriculum</h3>
        {course.curriculumItems.length === 0 ? (
          <p className="course-detail-empty">The curriculum for this course hasn't been published yet.</p>
        ) : (
          <ol className="curriculum-list">
            {course.curriculumItems.map((item) => (
              <li key={item.uuid} className="curriculum-item">
                <div className="curriculum-item-main">
                  <span className="curriculum-item-type">{item.type === 'QUIZ' ? 'Quiz' : 'Lecture'}</span>
                  <span className="curriculum-item-title">{item.title}</span>
                  {item.durationMinutes != null && (
                    <span className="curriculum-item-duration">{formatDuration(item.durationMinutes)}</span>
                  )}
                </div>
                {item.description && <p className="curriculum-item-description">{item.description}</p>}
              </li>
            ))}
          </ol>
        )}
      </section>

      <section className="course-detail-section" aria-labelledby="reviews-heading">
        <div className="course-detail-section-header">
          <h3 id="reviews-heading">Reviews</h3>
          {totalReviews > 0 && (
            <Link to={`/courses/${course.uuid}/reviews`} className="course-detail-all-reviews">
              See all {totalReviews} {totalReviews === 1 ? 'review' : 'reviews'}
            </Link>
          )}
        </div>
        {summary && <RatingBreakdown summary={summary} />}
        {recentReviews.length === 0 ? (
          <p className="course-detail-empty">No reviews yet.</p>
        ) : (
          <>
            <h4 className="course-detail-subheading">Most recent</h4>
            <ul className="review-list">
              {recentReviews.map((review) => (
                <ReviewCard key={review.uuid} review={review} />
              ))}
            </ul>
          </>
        )}
      </section>
    </article>
  );
}
