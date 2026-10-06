import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import CourseCard from './CourseCard';

describe('CourseCard', () => {
  it('links the course title to the course detail page', () => {
    render(
      <MemoryRouter>
        <CourseCard
          course={{
            uuid: 'abc-123',
            name: 'Java Basics',
            description: 'Learn Java',
            teacherName: 'bob',
            rating: 4,
            numberOfStudents: 1,
            category: null,
            createdDate: null,
          }}
        />
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Java Basics' })).toHaveAttribute('href', '/courses/abc-123');
    expect(screen.getByText('1 student')).toBeInTheDocument();
  });
});
