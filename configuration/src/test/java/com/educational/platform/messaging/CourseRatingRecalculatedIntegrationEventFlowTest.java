package com.educational.platform.messaging;

import java.util.UUID;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the messaging flow between the course-reviews and courses bounded contexts:
 * {@link CourseRatingRecalculatedIntegrationEvent} updates the rating of the course.
 */
@Sql(scripts = "classpath:messaging/course_rating_flow.sql")
public class CourseRatingRecalculatedIntegrationEventFlowTest extends MessagingIntegrationTestSupport {

	private final UUID courseId = UUID.fromString("223e4567-e89b-12d3-a456-426655440303");

	@Autowired
	private CourseRepository courseRepository;

	@Test
	void courseRatingRecalculatedEvent_courseRatingUpdatedInCoursesContext() {
		// when
		eventPublisher.publishEvent(new CourseRatingRecalculatedIntegrationEvent(courseId, 4.5));

		// then
		awaitAsyncListeners(() ->
				assertThat(courseRepository.findByUuid(courseId).orElseThrow())
						.hasFieldOrPropertyWithValue("rating", new CourseRating(4.5)));
	}
}
