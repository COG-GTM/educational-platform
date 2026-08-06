import { FormEvent, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  CatalogFacets,
  CatalogSort,
  CourseCatalogPage,
  fetchCatalog,
  fetchCatalogFacets,
} from '../api/catalog';
import CourseCard from '../components/CourseCard';
import Pagination from '../components/Pagination';

const SORT_OPTIONS: { value: CatalogSort; label: string }[] = [
  { value: 'NEWEST', label: 'Newest' },
  { value: 'RATING', label: 'Highest rated' },
  { value: 'POPULARITY', label: 'Most popular' },
];

const MIN_RATING_OPTIONS = ['1', '2', '3', '4', '4.5'];

type LoadState = 'loading' | 'loaded' | 'error';

export default function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [data, setData] = useState<CourseCatalogPage | null>(null);
  const [facets, setFacets] = useState<CatalogFacets>({ categories: [], teachers: [] });
  const [state, setState] = useState<LoadState>('loading');
  const [searchInput, setSearchInput] = useState(searchParams.get('search') ?? '');
  const [reloadToken, setReloadToken] = useState(0);

  const search = searchParams.get('search') ?? '';
  const category = searchParams.get('category') ?? '';
  const teacher = searchParams.get('teacher') ?? '';
  const rawSort = searchParams.get('sort');
  const sort: CatalogSort = SORT_OPTIONS.some((o) => o.value === rawSort) ? (rawSort as CatalogSort) : 'NEWEST';
  const rawMinRating = searchParams.get('minRating') ?? '';
  const minRating = Number.isFinite(Number(rawMinRating)) && rawMinRating !== '' ? rawMinRating : '';
  const page = Math.max(0, Math.floor(Number(searchParams.get('page') ?? '0') || 0));

  const hasActiveFilters = Boolean(search || category || teacher || minRating);

  useEffect(() => {
    setSearchInput(search);
  }, [search]);

  useEffect(() => {
    fetchCatalogFacets()
      .then(setFacets)
      .catch(() => setFacets({ categories: [], teachers: [] }));
  }, []);

  useEffect(() => {
    let cancelled = false;
    setState('loading');
    fetchCatalog({ search, category, teacher, minRating, sort, page })
      .then((result) => {
        if (cancelled) return;
        if (result.items.length === 0 && result.totalElements > 0 && page > 0) {
          const lastPage = Math.max(0, result.totalPages - 1);
          updateParams({ page: lastPage > 0 ? String(lastPage) : '' });
          return;
        }
        setData(result);
        setState('loaded');
      })
      .catch(() => {
        if (cancelled) return;
        setState('error');
      });
    return () => {
      cancelled = true;
    };
  }, [search, category, teacher, minRating, sort, page, reloadToken]);

  const updateParams = (updates: Record<string, string>) => {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(updates)) {
      if (value) {
        next.set(key, value);
      } else {
        next.delete(key);
      }
    }
    setSearchParams(next);
  };

  const onSearchSubmit = (event: FormEvent) => {
    event.preventDefault();
    updateParams({ search: searchInput.trim(), page: '' });
  };

  const clearFilters = () => {
    setSearchInput('');
    const next = new URLSearchParams();
    if (sort !== 'NEWEST') {
      next.set('sort', sort);
    }
    setSearchParams(next);
  };

  return (
    <section className="catalog">
      <h2>Course Catalog</h2>

      <form className="catalog-search" onSubmit={onSearchSubmit} role="search">
        <input
          type="search"
          placeholder="Search courses by title or description"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          aria-label="Search courses"
        />
        <button type="submit">Search</button>
      </form>

      <div className="catalog-filters">
        <label>
          Category
          <select value={category} onChange={(e) => updateParams({ category: e.target.value, page: '' })}>
            <option value="">All categories</option>
            {facets.categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
        <label>
          Teacher
          <select value={teacher} onChange={(e) => updateParams({ teacher: e.target.value, page: '' })}>
            <option value="">All teachers</option>
            {facets.teachers.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label>
          Minimum rating
          <select value={minRating} onChange={(e) => updateParams({ minRating: e.target.value, page: '' })}>
            <option value="">Any rating</option>
            {MIN_RATING_OPTIONS.map((r) => (
              <option key={r} value={r}>
                {r}+ stars
              </option>
            ))}
          </select>
        </label>
        <label>
          Sort by
          <select value={sort} onChange={(e) => updateParams({ sort: e.target.value, page: '' })}>
            {SORT_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </label>
        {hasActiveFilters && (
          <button type="button" className="catalog-clear-filters" onClick={clearFilters}>
            Clear filters
          </button>
        )}
      </div>

      {state === 'loading' && (
        <div className="catalog-state" role="status">
          <div className="spinner" aria-hidden="true" />
          <p>Loading courses…</p>
        </div>
      )}

      {state === 'error' && (
        <div className="catalog-state catalog-error" role="alert">
          <p>We couldn't load the catalog. Please try again.</p>
          <button type="button" onClick={() => setReloadToken((token) => token + 1)}>
            Retry
          </button>
        </div>
      )}

      {state === 'loaded' && data && (
        <>
          <p className="catalog-total" role="status">
            {data.totalElements} {data.totalElements === 1 ? 'course' : 'courses'} found
          </p>

          {data.items.length === 0 ? (
            <div className="catalog-state catalog-empty">
              <p>No courses match your search or filters.</p>
              <button type="button" onClick={clearFilters}>
                Clear filters
              </button>
            </div>
          ) : (
            <div className="catalog-grid">
              {data.items.map((course) => (
                <CourseCard key={course.uuid} course={course} />
              ))}
            </div>
          )}

          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            onPageChange={(nextPage) => updateParams({ page: nextPage > 0 ? String(nextPage) : '' })}
          />
        </>
      )}
    </section>
  );
}
