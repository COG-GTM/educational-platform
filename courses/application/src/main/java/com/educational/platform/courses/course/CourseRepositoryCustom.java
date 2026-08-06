package com.educational.platform.courses.course;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.query.Param;

import com.educational.platform.courses.course.catalog.CourseCatalogPageDTO;
import com.educational.platform.courses.course.catalog.CourseCatalogQuery;

public interface CourseRepositoryCustom {

	/**
	 * Retrieves a course dto by its uuid.
	 *
	 * @param uuid must not be {@literal null}.
	 * @return the course dto with the given uuid or {@literal Optional#empty()} if none found.
	 * @throws IllegalArgumentException if {@literal uuid} is {@literal null}.
	 */
	Optional<CourseDTO> findDTOByUuid(@Param("uuid") UUID uuid);

	/**
	 * Retrieves a page of published courses matching the catalog query.
	 *
	 * @param query catalog query with search, filters, sorting and pagination.
	 * @return the page of catalog course dtos.
	 */
	CourseCatalogPageDTO findCatalog(CourseCatalogQuery query);

}
