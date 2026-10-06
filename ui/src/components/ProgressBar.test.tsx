import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ProgressBar from './ProgressBar';

describe('ProgressBar', () => {
  it('exposes the percentage through progressbar aria attributes and fills the bar', () => {
    render(<ProgressBar percent={50} label="Java Basics progress" />);

    const bar = screen.getByRole('progressbar', { name: 'Java Basics progress' });
    expect(bar).toHaveAttribute('aria-valuemin', '0');
    expect(bar).toHaveAttribute('aria-valuemax', '100');
    expect(bar).toHaveAttribute('aria-valuenow', '50');
    expect(bar.querySelector('.progress-bar-fill')).toHaveStyle({ width: '50%' });
  });

  it('rounds fractional percentages to the nearest integer', () => {
    render(<ProgressBar percent={66.6} label="progress" />);

    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '67');
  });

  it('clamps values below 0 and above 100', () => {
    const { rerender } = render(<ProgressBar percent={-20} label="progress" />);
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '0');
    expect(screen.getByRole('progressbar').querySelector('.progress-bar-fill')).toHaveStyle({ width: '0%' });

    rerender(<ProgressBar percent={250} label="progress" />);
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
    expect(screen.getByRole('progressbar').querySelector('.progress-bar-fill')).toHaveStyle({ width: '100%' });
  });
});
