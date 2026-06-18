package com.educational.platform.courses.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.query.Param;

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
	 * Retrieves courses whose name or description matches the given keyword.
	 *
	 * @param keyword search keyword.
	 * @return the list of matching course dtos.
	 */
	List<CourseLightDTO> searchByKeyword(@Param("keyword") String keyword);

}
