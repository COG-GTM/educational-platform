package com.educational.platform.courses.course.catalog;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.educational.platform.courses.course.CourseRepository;

/**
 * Query handler for browsing the catalog of published courses.
 */
@Component
public class CourseCatalogQueryHandler {

	private final CourseRepository repository;

	public CourseCatalogQueryHandler(CourseRepository repository) {
		this.repository = repository;
	}

	/**
	 * Retrieves a page of published courses matching the query.
	 *
	 * @param query query.
	 * @return corresponding page of catalog course dtos.
	 */
	@Nonnull
	@Transactional(readOnly = true)
	public CourseCatalogPageDTO handle(CourseCatalogQuery query) {
		return repository.findCatalog(query);
	}

}
