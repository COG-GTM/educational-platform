import { describe, expect, it } from 'vitest';
import { formatDate, formatDuration } from './format';

describe('formatDate', () => {
  it('formats an ISO timestamp using the long-month date format', () => {
    const expected = new Date('2026-09-01T10:00:00').toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
    expect(formatDate('2026-09-01T10:00:00')).toBe(expected);
    expect(formatDate('2026-09-01T10:00:00')).toContain('2026');
  });

  it('returns the raw value when it is not a parseable date', () => {
    expect(formatDate('not-a-date')).toBe('not-a-date');
    expect(formatDate('')).toBe('');
  });
});

describe('formatDuration', () => {
  it('shows minutes only below one hour', () => {
    expect(formatDuration(0)).toBe('0 min');
    expect(formatDuration(45)).toBe('45 min');
    expect(formatDuration(59)).toBe('59 min');
  });

  it('shows whole hours without a minutes remainder', () => {
    expect(formatDuration(60)).toBe('1 h');
    expect(formatDuration(120)).toBe('2 h');
  });

  it('shows hours and the remaining minutes', () => {
    expect(formatDuration(61)).toBe('1 h 1 min');
    expect(formatDuration(150)).toBe('2 h 30 min');
  });
});
