import { CourseEnrollment, enrollmentStatus, EnrollmentStatusFilter } from '../api/enrollments';

export const STATUS_LABELS: Record<EnrollmentStatusFilter, string> = {
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  ARCHIVED: 'Archived',
};

export default function EnrollmentStatusBadge({ enrollment }: { enrollment: CourseEnrollment }) {
  const status = enrollmentStatus(enrollment);
  return <span className={`status-badge status-badge-${status.toLowerCase().replace('_', '-')}`}>{STATUS_LABELS[status]}</span>;
}
