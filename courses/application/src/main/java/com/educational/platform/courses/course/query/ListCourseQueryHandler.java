package com.educational.platform.courses.course.query;

import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.courses.course.CourseRepository;

/**
 * Query handler for getting the list of courses.
 */
@Component
public class ListCourseQueryHandler {

	private final CourseRepository repository;

    public ListCourseQueryHandler(CourseRepository repository) {
        this.repository = repository;
    }

    /**
	 * Retrieves a page of course dtos.
	 *
	 * @param query query.
	 * @return corresponding page of course dtos.
	 */
	@Nonnull
	@Transactional(readOnly = true)
	public Page<CourseLightDTO> handle(ListCourseQuery query) {
		return repository.list(PageRequest.of(query.page(), query.size()));
	}

}
