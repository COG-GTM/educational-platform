import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import RatingBreakdown from './RatingBreakdown';

describe('RatingBreakdown', () => {
  it('shows the average to one decimal, the review count and one bar per star from 5 down to 1', () => {
    const { container } = render(
      <RatingBreakdown
        summary={{ averageRating: 4.25, totalReviews: 8, ratingCounts: { '1': 0, '2': 1, '3': 1, '4': 2, '5': 4 } }}
      />,
    );

    expect(container.querySelector('.rating-breakdown-number')).toHaveTextContent('4.3');
    expect(screen.getByText('8 reviews')).toBeInTheDocument();

    const list = screen.getByRole('list', { name: 'Rating breakdown' });
    const rows = within(list).getAllByRole('listitem');
    expect(rows.map((row) => row.textContent)).toEqual([
      '5 stars4',
      '4 stars2',
      '3 stars1',
      '2 stars1',
      '1 star0',
    ]);

    const meters = within(list).getAllByRole('meter');
    expect(meters.map((m) => m.getAttribute('aria-valuenow'))).toEqual(['50', '25', '13', '13', '0']);
    expect(meters[0]).toHaveAttribute('aria-label', '4 reviews with 5 stars');
    expect(meters[2]).toHaveAttribute('aria-label', '1 review with 3 stars');
    expect(meters[4]).toHaveAttribute('aria-label', '0 reviews with 1 star');
    expect(meters[0].firstElementChild).toHaveStyle({ width: '50%' });
  });

  it('uses the singular label for exactly one review', () => {
    render(<RatingBreakdown summary={{ averageRating: 5, totalReviews: 1, ratingCounts: { '5': 1 } }} />);

    expect(screen.getByText('1 review')).toBeInTheDocument();
    expect(screen.getByRole('meter', { name: '1 review with 5 stars' })).toHaveAttribute('aria-valuenow', '100');
  });

  it('treats missing star levels as zero and renders no fill for an empty summary', () => {
    const { container } = render(<RatingBreakdown summary={{ averageRating: 0, totalReviews: 0, ratingCounts: {} }} />);

    expect(container.querySelector('.rating-breakdown-number')).toHaveTextContent('0.0');
    expect(screen.getByText('0 reviews')).toBeInTheDocument();
    const meters = screen.getAllByRole('meter');
    expect(meters).toHaveLength(5);
    meters.forEach((meter) => {
      expect(meter).toHaveAttribute('aria-valuenow', '0');
      expect(meter.firstElementChild).toHaveStyle({ width: '0%' });
    });
  });
});
