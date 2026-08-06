package com.educational.platform.courses.course.catalog;

/**
 * Query for browsing the catalog of published courses.
 */
public record CourseCatalogQuery(String search, String category, String teacher, Double minRating,
								 CourseCatalogSort sort, int page, int size) {

}
