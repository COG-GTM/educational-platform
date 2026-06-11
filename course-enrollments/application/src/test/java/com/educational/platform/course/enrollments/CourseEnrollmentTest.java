package com.educational.platform.course.enrollments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

public class CourseEnrollmentTest {

	@Test
	void constructor_validArguments_initializesWithInProgressStatus() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
	}

	@Test
	void constructor_validArguments_generatesNonNullUUID() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

		// then
		assertThat(enrollment.getUuid()).isNotNull();
	}

	@Test
	void constructor_multipleInstances_generateUniqueUUIDs() {
		// given / when
		final CourseEnrollment enrollment1 = new CourseEnrollment(1, 2);
		final CourseEnrollment enrollment2 = new CourseEnrollment(1, 2);

		// then
		assertThat(enrollment1.getUuid()).isNotEqualTo(enrollment2.getUuid());
	}

	@Test
	void complete_completedStatus() {
		// given
		final CreateCourseCommand createCourseCommand = new CreateCourseCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
		final CreateStudentCommand createStudentCommand = new CreateStudentCommand("username");
		final CourseEnrollment enrollment = new CourseEnrollment(
				new EnrollCourse(createCourseCommand).getId(),
				new Student(createStudentCommand).getId()
		);

		// when
		enrollment.complete();

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
	}

	@Test
	void complete_alreadyInProgress_transitionsToCompleted() {
		// given
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);

		// when
		enrollment.complete();

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
	}

	@Test
	void complete_alreadyCompleted_remainsCompleted() {
		// given
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
		enrollment.complete();
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);

		// when
		enrollment.complete();

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
	}

	@Test
	void constructor_storesCourseAndStudentReferences() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(42, 99);

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", 42);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", 99);
	}

	@Test
	void constructor_idIsNullBeforePersist() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("id", null);
	}

	@Test
	void getUuid_returnsValidUuidFormat() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

		// then
		assertThat(enrollment.getUuid()).isInstanceOf(UUID.class);
		assertThat(enrollment.getUuid().toString()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	}

	@Test
	void constructor_nullArguments_acceptsNulls() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(null, null);

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", null);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", null);
		assertThat(enrollment.getUuid()).isNotNull();
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
	}

}
