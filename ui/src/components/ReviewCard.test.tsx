import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { CourseReview } from '../api/courses';
import ReviewCard from './ReviewCard';
import { formatDate } from '../utils/format';

const base: CourseReview = { uuid: 'r-1', course: 'c-1', username: 'carol', rating: 4, comment: 'Great course', createdDate: '2026-09-03T10:00:00' };

function renderCard(review: Partial<CourseReview>) {
  return render(
    <ul>
      <ReviewCard review={{ ...base, ...review }} />
    </ul>,
  );
}

describe('ReviewCard', () => {
  it('shows the author, rating, formatted creation date and comment', () => {
    const { container } = renderCard({});

    expect(screen.getByText('carol')).toBeInTheDocument();
    expect(screen.getByLabelText('Rated 4.0 out of 5')).toBeInTheDocument();
    expect(screen.getByText('Great course')).toBeInTheDocument();
    const time = container.querySelector('time');
    expect(time).toHaveAttribute('dateTime', '2026-09-03T10:00:00');
    expect(time).toHaveTextContent(formatDate('2026-09-03T10:00:00'));
  });

  it('shows a placeholder when the review has no written comment', () => {
    renderCard({ comment: null });

    expect(screen.getByText('No written comment.')).toHaveClass('review-card-comment-empty');
  });

  it('omits the date when the review has no creation date', () => {
    const { container } = renderCard({ createdDate: null });

    expect(container.querySelector('time')).toBeNull();
    expect(screen.getByText('Great course')).toBeInTheDocument();
  });
});
