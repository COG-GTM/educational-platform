import { CourseCatalogItem } from '../api/catalog';
import StarRating from './StarRating';

interface CourseCardProps {
  course: CourseCatalogItem;
}

export default function CourseCard({ course }: CourseCardProps) {
  return (
    <article className="course-card">
      <div className="course-card-header">
        <h3 className="course-card-title">{course.name}</h3>
        {course.category && <span className="course-card-category">{course.category}</span>}
      </div>
      <p className="course-card-teacher">by {course.teacherName}</p>
      <div className="course-card-stats">
        <StarRating rating={course.rating} />
        <span className="course-card-students">
          {course.numberOfStudents} {course.numberOfStudents === 1 ? 'student' : 'students'}
        </span>
      </div>
      <p className="course-card-description">{course.description}</p>
    </article>
  );
}
