package com.educational.platform.course.enrollments.integration.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentEnrolledToCourseIntegrationEventTest {

	@Test
	void constructor_validArguments_storesCourseIdAndUsername() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student");

		// then
		assertThat(event.courseId()).isEqualTo(courseId);
		assertThat(event.username()).isEqualTo("student");
	}

	@Test
	void constructor_nullValues_acceptsNulls() {
		// when
		final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(null, null);

		// then
		assertThat(event.courseId()).isNull();
		assertThat(event.username()).isNull();
	}

	@Test
	void equals_sameFields_areEqual() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(courseId, "student");
		final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(courseId, "student");

		// then
		assertThat(event1).isEqualTo(event2);
	}

	@Test
	void equals_differentCourseId_areNotEqual() {
		// given
		final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID courseId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

		// when
		final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(courseId1, "student");
		final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(courseId2, "student");

		// then
		assertThat(event1).isNotEqualTo(event2);
	}

	@Test
	void equals_differentUsername_areNotEqual() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");
		final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(courseId, "student2");

		// then
		assertThat(event1).isNotEqualTo(event2);
	}

	@Test
	void hashCode_sameFields_sameHashCode() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(courseId, "student");
		final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(courseId, "student");

		// then
		assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
	}

	@Test
	void toString_containsFieldValues() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student");

		// then
		assertThat(event.toString()).contains(courseId.toString());
		assertThat(event.toString()).contains("student");
	}
}
