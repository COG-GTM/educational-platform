import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  CourseEnrollment,
  CourseEnrollmentPage,
  EnrollmentStatusFilter,
  fetchMyEnrollments,
  setEnrollmentArchived,
} from '../api/enrollments';
import { useAuth } from '../auth/AuthContext';
import EnrollmentStatusBadge, { STATUS_LABELS } from '../components/EnrollmentStatusBadge';
import Pagination from '../components/Pagination';
import ProgressBar from '../components/ProgressBar';
import { formatDate } from '../utils/format';

const TABS: EnrollmentStatusFilter[] = ['IN_PROGRESS', 'COMPLETED', 'ARCHIVED'];

const EMPTY_TAB_MESSAGES: Record<EnrollmentStatusFilter, string> = {
  IN_PROGRESS: 'Nothing in progress right now. Pick up a completed course again or find a new one in the catalog.',
  COMPLETED: "You haven't completed a course yet. Keep going — every lecture counts.",
  ARCHIVED: 'No archived courses. Archive a course to hide it from your active list without losing progress.',
};

type LoadState = 'loading' | 'loaded' | 'error';

interface Notice {
  text: string;
  undo?: { enrollment: CourseEnrollment; archived: boolean };
}

export default function MyLearningPage() {
  const { user, isStudent } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [data, setData] = useState<CourseEnrollmentPage | null>(null);
  const [state, setState] = useState<LoadState>('loading');
  const [reloadToken, setReloadToken] = useState(0);
  const [pendingUuid, setPendingUuid] = useState<string | null>(null);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const rawStatus = searchParams.get('status');
  const status: EnrollmentStatusFilter = TABS.includes(rawStatus as EnrollmentStatusFilter)
    ? (rawStatus as EnrollmentStatusFilter)
    : 'IN_PROGRESS';
  const page = Math.max(0, Math.floor(Number(searchParams.get('page') ?? '0') || 0));

  useEffect(() => {
    if (!user || !isStudent) return;
    let cancelled = false;
    setState('loading');
    fetchMyEnrollments({ status, page })
      .then((result) => {
        if (cancelled) return;
        const lastPage = Math.max(0, result.totalPages - 1);
        if (result.items.length === 0 && result.totalElements > 0 && page > lastPage) {
          setSearchParams(
            (prev) => {
              const next = new URLSearchParams(prev);
              if (lastPage > 0) {
                next.set('page', String(lastPage));
              } else {
                next.delete('page');
              }
              return next;
            },
            { replace: true },
          );
          return;
        }
        setData(result);
        setState('loaded');
      })
      .catch(() => {
        if (cancelled) return;
        setState('error');
      });
    return () => {
      cancelled = true;
    };
  }, [user, isStudent, status, page, reloadToken, setSearchParams]);

  const selectTab = (next: EnrollmentStatusFilter) => {
    const params = new URLSearchParams();
    if (next !== 'IN_PROGRESS') params.set('status', next);
    setSearchParams(params);
  };

  const changePage = (next: number) => {
    const params = new URLSearchParams(searchParams);
    if (next > 0) {
      params.set('page', String(next));
    } else {
      params.delete('page');
    }
    setSearchParams(params);
  };

  const changeArchived = async (enrollment: CourseEnrollment, archived: boolean, isUndo = false) => {
    setPendingUuid(enrollment.uuid);
    setActionError(null);
    try {
      await setEnrollmentArchived(enrollment.uuid, archived);
      const name = enrollment.courseName ?? 'Course';
      if (isUndo) {
        setNotice({ text: archived ? `"${name}" is archived again.` : `"${name}" is back in your active list.` });
      } else {
        setNotice({
          text: archived
            ? `"${name}" was archived. You can restore it any time from the Archived tab.`
            : `"${name}" was restored to your active list.`,
          undo: { enrollment, archived: !archived },
        });
      }
      setReloadToken((token) => token + 1);
    } catch {
      setActionError(`We couldn't ${archived ? 'archive' : 'restore'} "${enrollment.courseName ?? 'this course'}". Please try again.`);
    } finally {
      setPendingUuid(null);
    }
  };

  if (!user) {
    return (
      <section className="learning">
        <h2>My Learning</h2>
        <div className="catalog-state">
          <p>Sign in to see your enrollments and track your progress.</p>
          <Link to={`/login?redirect=${encodeURIComponent('/learning')}`} className="button-link">
            Sign in
          </Link>
        </div>
      </section>
    );
  }

  if (!isStudent) {
    return (
      <section className="learning">
        <h2>My Learning</h2>
        <div className="catalog-state">
          <p>Only students have a learning dashboard.</p>
          <Link to="/catalog" className="button-link">
            Browse the catalog
          </Link>
        </div>
      </section>
    );
  }

  const counts = data?.counts;
  const totalEnrollments = counts ? counts.inProgress + counts.completed + counts.archived : null;

  return (
    <section className="learning">
      <h2>My Learning</h2>

      {state === 'loaded' && totalEnrollments === 0 ? (
        <div className="catalog-state learning-empty">
          <h3>You haven't enrolled in any courses yet</h3>
          <p>Browse the catalog and enroll in a course to start tracking your progress here.</p>
          <Link to="/catalog" className="button-link">
            Explore the catalog
          </Link>
        </div>
      ) : (
        <>
          <div className="learning-tabs" role="tablist" aria-label="Enrollment status">
            {TABS.map((tab) => (
              <button
                key={tab}
                type="button"
                role="tab"
                aria-selected={tab === status}
                className={tab === status ? 'learning-tab learning-tab-active' : 'learning-tab'}
                onClick={() => selectTab(tab)}
              >
                {STATUS_LABELS[tab]}
                {counts && <span className="learning-tab-count">{counts[tabCountKey(tab)]}</span>}
              </button>
            ))}
          </div>

          <p className="course-detail-cta-status learning-notice" role="status" aria-live="polite">
            {notice?.text}
            {notice?.undo && (
              <>
                {' '}
                <button
                  type="button"
                  className="link-button"
                  disabled={pendingUuid !== null}
                  onClick={() => notice.undo && changeArchived(notice.undo.enrollment, notice.undo.archived, true)}
                >
                  Undo
                </button>
              </>
            )}
          </p>
          {actionError && (
            <p className="form-error" role="alert">
              {actionError}
            </p>
          )}

          {state === 'loading' && (
            <div className="catalog-state" role="status">
              <div className="spinner" aria-hidden="true" />
              <p>Loading your courses…</p>
            </div>
          )}

          {state === 'error' && (
            <div className="catalog-state catalog-error" role="alert">
              <p>We couldn't load your courses. Please try again.</p>
              <button type="button" onClick={() => setReloadToken((token) => token + 1)}>
                Retry
              </button>
            </div>
          )}

          {state === 'loaded' && data && data.items.length === 0 && (
            <div className="catalog-state">
              <p>{EMPTY_TAB_MESSAGES[status]}</p>
              <Link to="/catalog" className="button-link">
                Browse the catalog
              </Link>
            </div>
          )}

          {state === 'loaded' && data && data.items.length > 0 && (
            <>
              <ul className="learning-grid" aria-label={`${STATUS_LABELS[status]} courses`}>
                {data.items.map((enrollment) => (
                  <li key={enrollment.uuid} className="course-card learning-card">
                    <div className="course-card-header">
                      <h3>
                        <Link to={`/learning/${enrollment.uuid}`}>{enrollment.courseName ?? 'Untitled course'}</Link>
                      </h3>
                      <EnrollmentStatusBadge enrollment={enrollment} />
                    </div>
                    <ProgressBar
                      percent={enrollment.progressPercent}
                      label={`${enrollment.courseName ?? 'Course'} progress`}
                    />
                    <p className="learning-progress-text">
                      {enrollment.progressPercent}% · {enrollment.completedLectures} of {enrollment.totalLectures} lectures
                      completed
                    </p>
                    <p className="learning-meta">
                      {enrollment.completionStatus === 'COMPLETED' && enrollment.completedAt
                        ? `Completed on ${formatDate(enrollment.completedAt)}`
                        : enrollment.lastActivityAt
                          ? `Last activity ${formatDate(enrollment.lastActivityAt)}`
                          : 'No activity yet'}
                    </p>
                    <div className="learning-card-actions">
                      <Link to={`/learning/${enrollment.uuid}`} className="button-link">
                        {enrollment.archived
                          ? 'View course'
                          : enrollment.completionStatus === 'COMPLETED'
                            ? 'Review course'
                            : 'Continue learning'}
                      </Link>
                      <button
                        type="button"
                        className="button-secondary"
                        disabled={pendingUuid !== null}
                        onClick={() => changeArchived(enrollment, !enrollment.archived)}
                      >
                        {pendingUuid === enrollment.uuid ? 'Saving…' : enrollment.archived ? 'Restore' : 'Archive'}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
              <Pagination page={data.page} totalPages={data.totalPages} onPageChange={changePage} label="Enrollment pages" />
            </>
          )}
        </>
      )}
    </section>
  );
}

function tabCountKey(tab: EnrollmentStatusFilter): 'inProgress' | 'completed' | 'archived' {
  if (tab === 'IN_PROGRESS') return 'inProgress';
  return tab === 'COMPLETED' ? 'completed' : 'archived';
}
