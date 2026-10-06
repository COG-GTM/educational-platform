import { CourseReviewSummary } from '../api/courses';
import StarRating from './StarRating';

interface RatingBreakdownProps {
  summary: CourseReviewSummary;
}

const STARS = [5, 4, 3, 2, 1];

export default function RatingBreakdown({ summary }: RatingBreakdownProps) {
  const total = summary.totalReviews;
  return (
    <div className="rating-breakdown">
      <div className="rating-breakdown-average">
        <span className="rating-breakdown-number">{summary.averageRating.toFixed(1)}</span>
        <StarRating rating={summary.averageRating} />
        <span className="rating-breakdown-total">
          {total} {total === 1 ? 'review' : 'reviews'}
        </span>
      </div>
      <ul className="rating-breakdown-bars" aria-label="Rating breakdown">
        {STARS.map((star) => {
          const count = summary.ratingCounts[String(star)] ?? 0;
          const percent = total === 0 ? 0 : Math.round((count / total) * 100);
          return (
            <li key={star} className="rating-breakdown-row">
              <span className="rating-breakdown-label">
                {star} {star === 1 ? 'star' : 'stars'}
              </span>
              <span
                className="rating-breakdown-bar"
                role="meter"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={percent}
                aria-label={`${count} ${count === 1 ? 'review' : 'reviews'} with ${star} ${star === 1 ? 'star' : 'stars'}`}
              >
                <span className="rating-breakdown-fill" style={{ width: `${percent}%` }} />
              </span>
              <span className="rating-breakdown-count" aria-hidden="true">
                {count}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
