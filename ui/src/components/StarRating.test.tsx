import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import StarRating from './StarRating';

describe('StarRating', () => {
  it('rounds to the nearest half star', () => {
    const { container } = render(<StarRating rating={3.3} />);
    expect(container.querySelectorAll('.star-full')).toHaveLength(3);
    expect(container.querySelectorAll('.star-half')).toHaveLength(1);
    expect(container.querySelectorAll('.star-empty')).toHaveLength(1);
  });

  it('renders all full stars for a 5.0 rating', () => {
    const { container } = render(<StarRating rating={5} />);
    expect(container.querySelectorAll('.star-full')).toHaveLength(5);
  });

  it('renders all empty stars for a 0 rating and exposes an accessible label', () => {
    const { container } = render(<StarRating rating={0} />);
    expect(container.querySelectorAll('.star-empty')).toHaveLength(5);
    expect(screen.getByLabelText('Rated 0.0 out of 5')).toBeInTheDocument();
  });
});
