package com.educational.platform.courses.course.query;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;

/**
 * Query handler for searching courses by keyword.
 */
@Component
public class SearchCourseQueryHandler {

	private final CourseRepository repository;

	public SearchCourseQueryHandler(CourseRepository repository) {
		this.repository = repository;
	}

	/**
	 * Retrieves courses matching the keyword.
	 *
	 * @param query query.
	 * @return corresponding list of course dtos.
	 */
	@Nonnull
	public List<CourseLightDTO> handle(SearchCourseQuery query) {
		return repository.searchByKeyword(query.keyword());
	}

}
