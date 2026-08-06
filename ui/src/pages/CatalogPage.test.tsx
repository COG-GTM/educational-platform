import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { CourseCatalogPage } from '../api/catalog';
import CatalogPage from './CatalogPage';

const emptyFacets = { categories: [], teachers: [] };

function catalogPage(overrides: Partial<CourseCatalogPage> = {}): CourseCatalogPage {
  return {
    items: [],
    page: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0,
    ...overrides,
  };
}

function jsonResponse(body: unknown) {
  return {
    ok: true,
    json: () => Promise.resolve(body),
  } as Response;
}

function mockFetch(catalog: CourseCatalogPage | Error) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = String(input);
    if (url.includes('catalog-facets')) {
      return Promise.resolve(jsonResponse(emptyFacets));
    }
    if (catalog instanceof Error) {
      return Promise.reject(catalog);
    }
    return Promise.resolve(jsonResponse(catalog));
  });
}

function renderAt(url: string) {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <CatalogPage />
    </MemoryRouter>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('CatalogPage', () => {
  it('renders course cards and the total result count', async () => {
    mockFetch(
      catalogPage({
        items: [
          {
            uuid: '1',
            name: 'Java Basics',
            description: 'Learn Java',
            teacherName: 'teacher1',
            rating: 4.5,
            numberOfStudents: 10,
            category: 'Programming',
            createdDate: null,
          },
        ],
        totalElements: 1,
        totalPages: 1,
      }),
    );
    renderAt('/catalog');

    expect(await screen.findByText('Java Basics')).toBeInTheDocument();
    expect(screen.getByText(/1 course found/)).toBeInTheDocument();
  });

  it('shows the empty state when nothing matches', async () => {
    mockFetch(catalogPage());
    renderAt('/catalog?search=nomatch');

    expect(await screen.findByText(/No courses match/)).toBeInTheDocument();
    expect(screen.getAllByText('Clear filters').length).toBeGreaterThan(0);
  });

  it('shows the error state with a Retry button when the request fails', async () => {
    mockFetch(new Error('network'));
    renderAt('/catalog');

    expect(await screen.findByText(/couldn't load the catalog/)).toBeInTheDocument();
    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  it('requests the last valid page when the URL page is out of range', async () => {
    const fetchMock = mockFetch(catalogPage({ totalElements: 13, totalPages: 2 }));
    renderAt('/catalog?page=9');

    await waitFor(() => {
      const catalogCalls = fetchMock.mock.calls
        .map((call) => String(call[0]))
        .filter((url) => !url.includes('catalog-facets'));
      expect(catalogCalls.some((url) => url.includes('page=1'))).toBe(true);
    });
  });

  it('ignores invalid sort and minRating query values', async () => {
    const fetchMock = mockFetch(catalogPage());
    renderAt('/catalog?sort=bogus&minRating=abc');

    await waitFor(() => {
      const catalogCall = fetchMock.mock.calls
        .map((call) => String(call[0]))
        .find((url) => !url.includes('catalog-facets'));
      expect(catalogCall).toBeDefined();
      expect(catalogCall).toContain('sort=NEWEST');
      expect(catalogCall).not.toContain('minRating');
    });
  });
});
