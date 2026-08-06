package com.educational.platform.courses.course.catalog;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.educational.platform.courses.course.CourseRepository;

/**
 * Query handler for getting available filter values of the course catalog.
 */
@Component
public class CatalogFacetsQueryHandler {

	private final CourseRepository repository;

	public CatalogFacetsQueryHandler(CourseRepository repository) {
		this.repository = repository;
	}

	/**
	 * Retrieves categories and teachers of published courses.
	 *
	 * @return corresponding facets dto.
	 */
	@Nonnull
	@Transactional(readOnly = true)
	public CatalogFacetsDTO handle() {
		return new CatalogFacetsDTO(repository.publishedCategories(), repository.publishedTeachers());
	}

}
