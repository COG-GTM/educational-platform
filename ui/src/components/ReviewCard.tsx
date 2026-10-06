import { CourseReview } from '../api/courses';
import StarRating from './StarRating';
import { formatDate } from '../utils/format';

interface ReviewCardProps {
  review: CourseReview;
}

export default function ReviewCard({ review }: ReviewCardProps) {
  return (
    <li className="review-card">
      <div className="review-card-header">
        <span className="review-card-author">{review.username}</span>
        <StarRating rating={review.rating} />
      </div>
      {review.createdDate && (
        <time className="review-card-date" dateTime={review.createdDate}>
          {formatDate(review.createdDate)}
        </time>
      )}
      {review.comment ? (
        <p className="review-card-comment">{review.comment}</p>
      ) : (
        <p className="review-card-comment review-card-comment-empty">No written comment.</p>
      )}
    </li>
  );
}
