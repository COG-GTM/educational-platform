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
	void constructor_validArguments_enrollmentCreatedWithInProgressStatus() {
		// given / when
		final CourseEnrollment sut = new CourseEnrollment(1, 2);

		// then
		assertThat(sut)
				.hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS)
				.hasFieldOrPropertyWithValue("course", 1)
				.hasFieldOrPropertyWithValue("student", 2);
		assertThat(sut.getUuid()).isNotNull();
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
	void getUuid_afterCreation_returnsNonNullUuid() {
		// given
		final CourseEnrollment sut = new CourseEnrollment(1, 2);

		// when
		final UUID uuid = sut.getUuid();

		// then
		assertThat(uuid).isNotNull();
	}

	@Test
	void constructor_twoEnrollments_differentUuids() {
		// given / when
		final CourseEnrollment enrollment1 = new CourseEnrollment(1, 2);
		final CourseEnrollment enrollment2 = new CourseEnrollment(1, 2);

		// then
		assertThat(enrollment1.getUuid()).isNotEqualTo(enrollment2.getUuid());
	}

}
