export type CatalogSort = 'RATING' | 'POPULARITY' | 'NEWEST';

export interface CourseCatalogItem {
  uuid: string;
  name: string;
  description: string;
  teacherName: string;
  rating: number;
  numberOfStudents: number;
  category: string | null;
  createdDate: string | null;
}

export interface CourseCatalogPage {
  items: CourseCatalogItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CatalogFacets {
  categories: string[];
  teachers: string[];
}

export interface CatalogParams {
  search?: string;
  category?: string;
  teacher?: string;
  minRating?: string;
  sort?: CatalogSort;
  page?: number;
  size?: number;
}

async function get<T>(url: string): Promise<T> {
  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export function fetchCatalog(params: CatalogParams): Promise<CourseCatalogPage> {
  const query = new URLSearchParams();
  if (params.search) query.set('search', params.search);
  if (params.category) query.set('category', params.category);
  if (params.teacher) query.set('teacher', params.teacher);
  if (params.minRating) query.set('minRating', params.minRating);
  if (params.sort) query.set('sort', params.sort);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 12));
  return get<CourseCatalogPage>(`/api/courses?${query.toString()}`);
}

export function fetchCatalogFacets(): Promise<CatalogFacets> {
  return get<CatalogFacets>('/api/courses/catalog-facets');
}
