package com.educational.platform.courses.course.details;

import com.educational.platform.courses.course.CourseRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query handler for getting published course details with the ordered curriculum outline.
 */
@Component
public class CourseDetailsQueryHandler {

	private final CourseRepository repository;

	public CourseDetailsQueryHandler(CourseRepository repository) {
		this.repository = repository;
	}

	/**
	 * Retrieves details of a published course. Draft and archived courses are treated as absent.
	 *
	 * @param query query.
	 * @return course details or empty when the course is not published or does not exist.
	 */
	@Nonnull
	@Transactional(readOnly = true)
	public Optional<CourseDetailsDTO> handle(CourseDetailsQuery query) {
		return repository.findPublishedDetails(query.uuid())
				.map(details -> details.withCurriculumItems(repository.findCurriculum(query.uuid())));
	}
}
