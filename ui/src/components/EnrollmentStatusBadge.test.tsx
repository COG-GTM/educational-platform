import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { CourseEnrollment } from '../api/enrollments';
import EnrollmentStatusBadge, { STATUS_LABELS } from './EnrollmentStatusBadge';

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

describe('EnrollmentStatusBadge', () => {
  it('shows "In progress" for an active, unfinished enrollment', () => {
    render(<EnrollmentStatusBadge enrollment={enrollment()} />);

    const badge = screen.getByText('In progress');
    expect(badge).toHaveClass('status-badge', 'status-badge-in-progress');
  });

  it('shows "Completed" for an active, finished enrollment', () => {
    render(<EnrollmentStatusBadge enrollment={enrollment({ completionStatus: 'COMPLETED', completedAt: '2026-10-03T10:00:00' })} />);

    expect(screen.getByText('Completed')).toHaveClass('status-badge-completed');
  });

  it('shows "Archived" regardless of completion status', () => {
    const { rerender } = render(<EnrollmentStatusBadge enrollment={enrollment({ archived: true })} />);
    expect(screen.getByText('Archived')).toHaveClass('status-badge-archived');

    rerender(<EnrollmentStatusBadge enrollment={enrollment({ archived: true, completionStatus: 'COMPLETED' })} />);
    expect(screen.getByText('Archived')).toBeInTheDocument();
    expect(screen.queryByText('Completed')).not.toBeInTheDocument();
  });

  it('has a label for every dashboard status', () => {
    expect(STATUS_LABELS).toEqual({ IN_PROGRESS: 'In progress', COMPLETED: 'Completed', ARCHIVED: 'Archived' });
  });
});
