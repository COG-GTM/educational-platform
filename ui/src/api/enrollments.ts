import { get, HttpError, post, put } from './http';

export type CompletionStatus = 'IN_PROGRESS' | 'COMPLETED';
export type EnrollmentStatusFilter = 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED';

export interface CourseEnrollment {
  uuid: string;
  course: string;
  courseName: string | null;
  student: string;
  completionStatus: CompletionStatus;
  archived: boolean;
  completedLectures: number;
  totalLectures: number;
  progressPercent: number;
  enrolledAt: string | null;
  lastActivityAt: string | null;
  completedAt: string | null;
}

export interface LectureProgress {
  uuid: string;
  title: string;
  serialNumber: number | null;
  completed: boolean;
}

export interface CourseEnrollmentDetails {
  enrollment: CourseEnrollment;
  lectures: LectureProgress[];
}

export interface CourseEnrollmentCounts {
  inProgress: number;
  completed: number;
  archived: number;
}

export interface CourseEnrollmentPage {
  items: CourseEnrollment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  counts: CourseEnrollmentCounts;
}

export interface EnrollmentListQuery {
  status?: EnrollmentStatusFilter;
  page?: number;
  size?: number;
}

export function enrollmentStatus(enrollment: CourseEnrollment): EnrollmentStatusFilter {
  if (enrollment.archived) return 'ARCHIVED';
  return enrollment.completionStatus === 'COMPLETED' ? 'COMPLETED' : 'IN_PROGRESS';
}

export function fetchMyEnrollments(query: EnrollmentListQuery = {}): Promise<CourseEnrollmentPage> {
  const params = new URLSearchParams();
  if (query.status) params.set('status', query.status);
  if (query.page) params.set('page', String(query.page));
  if (query.size) params.set('size', String(query.size));
  const search = params.toString();
  return get<CourseEnrollmentPage>(`/api/course-enrollments${search ? `?${search}` : ''}`);
}

export function fetchEnrollment(uuid: string): Promise<CourseEnrollmentDetails> {
  return get<CourseEnrollmentDetails>(`/api/course-enrollments/${encodeURIComponent(uuid)}`);
}

/** Resolves to `null` when the current student is not enrolled in the course. */
export function fetchCurrentEnrollment(courseUuid: string): Promise<CourseEnrollment | null> {
  return get<CourseEnrollment>(`/api/courses/${encodeURIComponent(courseUuid)}/course-enrollments/current`).catch(
    (e: unknown) => {
      if (e instanceof HttpError && e.status === 404) return null;
      throw e;
    },
  );
}

export function enrollInCourse(uuid: string, student: string): Promise<string> {
  return post<string>(`/api/courses/${encodeURIComponent(uuid)}/course-enrollments`, { student });
}

export function updateLectureProgress(
  enrollmentUuid: string,
  lectureUuid: string,
  completed: boolean,
): Promise<CourseEnrollmentDetails> {
  return put<CourseEnrollmentDetails>(
    `/api/course-enrollments/${encodeURIComponent(enrollmentUuid)}/lectures/${encodeURIComponent(lectureUuid)}/progress`,
    { completed },
  );
}

export function setEnrollmentArchived(enrollmentUuid: string, archived: boolean): Promise<CourseEnrollment> {
  return put<CourseEnrollment>(`/api/course-enrollments/${encodeURIComponent(enrollmentUuid)}/archive-status`, {
    archived,
  });
}
