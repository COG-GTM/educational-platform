import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  CourseEnrollmentDetails,
  fetchEnrollment,
  setEnrollmentArchived,
  updateLectureProgress,
} from '../api/enrollments';
import { HttpError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import EnrollmentStatusBadge from '../components/EnrollmentStatusBadge';
import ProgressBar from '../components/ProgressBar';
import { formatDate } from '../utils/format';

type LoadState = 'loading' | 'ready' | 'not-found' | 'error';

export default function LearningCoursePage() {
  const { uuid = '' } = useParams();
  const { user, isStudent } = useAuth();

  const [state, setState] = useState<LoadState>('loading');
  const [details, setDetails] = useState<CourseEnrollmentDetails | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [pendingLecture, setPendingLecture] = useState<string | null>(null);
  const [archiving, setArchiving] = useState(false);
  const [celebrating, setCelebrating] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const activeUuid = useRef(uuid);
  activeUuid.current = uuid;

  useEffect(() => {
    if (!user || !isStudent) return;
    let cancelled = false;
    setState('loading');
    setDetails(null);
    setCelebrating(false);
    setMessage(null);
    setError(null);

    fetchEnrollment(uuid)
      .then((result) => {
        if (cancelled) return;
        setDetails(result);
        setState('ready');
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setState(e instanceof HttpError && e.status === 404 ? 'not-found' : 'error');
      });

    return () => {
      cancelled = true;
    };
  }, [user, isStudent, uuid, reloadToken]);

  const toggleLecture = async (lectureUuid: string, completed: boolean) => {
    if (!details) return;
    const target = uuid;
    const wasCompleted = details.enrollment.completionStatus === 'COMPLETED';
    const lecture = details.lectures.find((l) => l.uuid === lectureUuid);
    setPendingLecture(lectureUuid);
    setError(null);
    try {
      const updated = await updateLectureProgress(target, lectureUuid, completed);
      if (activeUuid.current !== target) return;
      setDetails(updated);
      const nowCompleted = updated.enrollment.completionStatus === 'COMPLETED';
      if (nowCompleted && !wasCompleted) {
        setCelebrating(true);
        setMessage(null);
      } else {
        if (!nowCompleted) setCelebrating(false);
        setMessage(
          completed
            ? `Marked "${lecture?.title ?? 'lecture'}" as completed.`
            : `Marked "${lecture?.title ?? 'lecture'}" as not completed.${wasCompleted ? ' The course is back in progress.' : ''}`,
        );
      }
    } catch (e: unknown) {
      if (activeUuid.current !== target) return;
      setError(
        e instanceof HttpError && e.status === 422
          ? 'This course is archived. Restore it to keep tracking progress.'
          : "We couldn't save your progress. Please try again.",
      );
    } finally {
      if (activeUuid.current === target) setPendingLecture(null);
    }
  };

  const changeArchived = async (archived: boolean) => {
    if (!details) return;
    const target = uuid;
    setArchiving(true);
    setError(null);
    try {
      const enrollment = await setEnrollmentArchived(target, archived);
      if (activeUuid.current !== target) return;
      setDetails((current) => (current ? { ...current, enrollment } : current));
      setMessage(
        archived
          ? 'This course is archived and hidden from your active list. Your progress is saved — restore it any time.'
          : 'This course is back in your active list.',
      );
    } catch {
      if (activeUuid.current === target) setError(`We couldn't ${archived ? 'archive' : 'restore'} this course. Please try again.`);
    } finally {
      if (activeUuid.current === target) setArchiving(false);
    }
  };

  if (!user) {
    return (
      <section className="learning">
        <div className="catalog-state">
          <p>Sign in to see your progress in this course.</p>
          <Link to={`/login?redirect=${encodeURIComponent(`/learning/${uuid}`)}`} className="button-link">
            Sign in
          </Link>
        </div>
      </section>
    );
  }

  if (!isStudent) {
    return (
      <section className="learning">
        <div className="catalog-state">
          <p>Only students can track course progress.</p>
          <Link to="/catalog" className="button-link">
            Browse the catalog
          </Link>
        </div>
      </section>
    );
  }

  if (state === 'loading') {
    return (
      <div className="catalog-state" role="status">
        <div className="spinner" aria-hidden="true" />
        <p>Loading your course…</p>
      </div>
    );
  }

  if (state === 'not-found') {
    return (
      <div className="catalog-state">
        <h2>We couldn't find this enrollment</h2>
        <p>It may belong to another account or no longer exist.</p>
        <Link to="/learning" className="button-link">
          Back to My Learning
        </Link>
      </div>
    );
  }

  if (state === 'error' || !details) {
    return (
      <div className="catalog-state catalog-error" role="alert">
        <p>We couldn't load this course. Please try again.</p>
        <button type="button" onClick={() => setReloadToken((token) => token + 1)}>
          Retry
        </button>
      </div>
    );
  }

  const { enrollment, lectures } = details;
  const courseName = enrollment.courseName ?? 'Untitled course';
  const locked = enrollment.archived;

  return (
    <article className="learning learning-course">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/learning">My Learning</Link> / <span aria-current="page">{courseName}</span>
      </nav>

      <header className="learning-course-header">
        <div>
          <h2>{courseName}</h2>
          <p className="learning-meta">
            <EnrollmentStatusBadge enrollment={enrollment} />{' '}
            {enrollment.enrolledAt && <span>Enrolled {formatDate(enrollment.enrolledAt)}</span>}
            {enrollment.lastActivityAt && <span> · Last activity {formatDate(enrollment.lastActivityAt)}</span>}
            {enrollment.completedAt && <span> · Completed {formatDate(enrollment.completedAt)}</span>}
          </p>
          <ProgressBar percent={enrollment.progressPercent} label="Course progress" />
          <p className="learning-progress-text" data-testid="progress-summary">
            {enrollment.progressPercent}% · {enrollment.completedLectures} of {enrollment.totalLectures} lectures completed
          </p>
        </div>
        <div className="course-detail-cta">
          <Link to={`/courses/${enrollment.course}`} className="button-link">
            Course details
          </Link>
          <button
            type="button"
            className="button-secondary"
            disabled={archiving}
            onClick={() => changeArchived(!enrollment.archived)}
          >
            {archiving ? 'Saving…' : enrollment.archived ? 'Restore course' : 'Archive course'}
          </button>
        </div>
      </header>

      {celebrating && (
        <div className="learning-celebration" role="status">
          <h3>🎉 Congratulations — you completed {courseName}!</h3>
          <p>Every lecture is done and this course now lives in your Completed list.</p>
          <div className="learning-card-actions">
            <Link to="/learning?status=COMPLETED" className="button-link button-enrolled">
              See completed courses
            </Link>
            <Link to="/catalog" className="button-link">
              Find your next course
            </Link>
          </div>
        </div>
      )}

      {locked && (
        <p className="learning-locked" role="note">
          This course is archived. Your progress is kept, but lectures can't be updated until you restore it.
        </p>
      )}

      <p className="course-detail-cta-status" role="status" aria-live="polite">
        {message}
      </p>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <section className="course-detail-section" aria-labelledby="learning-curriculum-heading">
        <h3 id="learning-curriculum-heading">Curriculum</h3>
        {lectures.length === 0 ? (
          <p className="course-detail-empty">This course has no lectures yet. Check back later.</p>
        ) : (
          <ol className="curriculum-list learning-lectures">
            {lectures.map((lecture) => (
              <li key={lecture.uuid} className={lecture.completed ? 'curriculum-item lecture-completed' : 'curriculum-item'}>
                <label className="lecture-toggle">
                  <input
                    type="checkbox"
                    checked={lecture.completed}
                    disabled={locked || pendingLecture !== null}
                    onChange={(e) => toggleLecture(lecture.uuid, e.target.checked)}
                    aria-label={`Mark "${lecture.title}" as ${lecture.completed ? 'not completed' : 'completed'}`}
                  />
                  <span className="curriculum-item-type">Lecture</span>
                  <span className="curriculum-item-title">{lecture.title}</span>
                  {pendingLecture === lecture.uuid && <span className="lecture-saving">Saving…</span>}
                  {lecture.completed && pendingLecture !== lecture.uuid && <span className="lecture-done">Completed</span>}
                </label>
              </li>
            ))}
          </ol>
        )}
      </section>
    </article>
  );
}
