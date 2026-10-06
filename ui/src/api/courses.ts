import { get } from './http';

export type CurriculumItemType = 'LECTURE' | 'QUIZ';

export interface CurriculumItem {
  uuid: string;
  title: string;
  description: string | null;
  serialNumber: number;
  type: CurriculumItemType;
  durationMinutes?: number | null;
}

export interface CourseDetails {
  uuid: string;
  name: string;
  description: string;
  category: string | null;
  teacherName: string;
  rating: number;
  numberOfStudents: number;
  publishedDate: string | null;
  curriculumItems: CurriculumItem[];
}

export interface CourseReview {
  uuid: string;
  course: string;
  username: string;
  comment: string | null;
  rating: number;
  createdDate: string | null;
}

export interface CourseReviewSummary {
  averageRating: number;
  totalReviews: number;
  ratingCounts: Record<string, number>;
}

export function fetchCourseDetails(uuid: string): Promise<CourseDetails> {
  return get<CourseDetails>(`/api/courses/${encodeURIComponent(uuid)}`);
}

export function fetchCourseReviews(uuid: string): Promise<CourseReview[]> {
  return get<CourseReview[]>(`/api/courses/${encodeURIComponent(uuid)}/reviews`);
}

export function fetchCourseReviewSummary(uuid: string): Promise<CourseReviewSummary> {
  return get<CourseReviewSummary>(`/api/courses/${encodeURIComponent(uuid)}/reviews/summary`);
}
