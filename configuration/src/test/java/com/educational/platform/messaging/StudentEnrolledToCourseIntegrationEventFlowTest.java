package com.educational.platform.messaging;

import java.util.UUID;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the messaging flow between the course-enrollments and courses bounded contexts:
 * {@link StudentEnrolledToCourseIntegrationEvent} increases the number of students of the course.
 */
@Sql(scripts = "classpath:messaging/student_enrolled_flow.sql")
public class StudentEnrolledToCourseIntegrationEventFlowTest extends MessagingIntegrationTestSupport {

	private final UUID courseId = UUID.fromString("223e4567-e89b-12d3-a456-426655440202");

	@Autowired
	private CourseRepository courseRepository;

	@Test
	void studentEnrolledToCourseEvent_numberOfStudentsIncreasedInCoursesContext() {
		// when
		eventPublisher.publishEvent(new StudentEnrolledToCourseIntegrationEvent(courseId, "student"));

		// then
		awaitAsyncListeners(() ->
				assertThat(courseRepository.findByUuid(courseId).orElseThrow())
						.hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1)));
	}
}
