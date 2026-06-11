package com.educational.platform.course.enrollments;

import com.educational.platform.common.domain.AggregateRoot;

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

	@Test
	void getUuid_multipleCalls_returnsSameValue() {
		// given
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

		// when
		final UUID first = enrollment.getUuid();
		final UUID second = enrollment.getUuid();

		// then
		assertThat(first).isEqualTo(second);
	}

	@Test
	void complete_uuidRemainsStableAfterStateChange() {
		// given
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
		final UUID uuidBeforeComplete = enrollment.getUuid();

		// when
		enrollment.complete();

		// then
		assertThat(enrollment.getUuid()).isEqualTo(uuidBeforeComplete);
	}

	@Test
	void constructor_differentCourseAndStudentIds_storesCorrectValues() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(10, 20);

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", 10);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", 20);
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
	}

	@Test
	void courseEnrollment_implementsAggregateRoot() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

		// then
		assertThat(enrollment).isInstanceOf(AggregateRoot.class);
	}

	@Test
	void getUuid_returnsVersion4Uuid() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(1, 2);

		// then — UUID.randomUUID() always returns version 4
		assertThat(enrollment.getUuid().version()).isEqualTo(4);
	}

	@Test
	void complete_doesNotMutateCourseOrStudentReferences() {
		// given
		final CourseEnrollment enrollment = new CourseEnrollment(42, 99);

		// when
		enrollment.complete();

		// then — course and student references remain unchanged after state transition
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", 42);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", 99);
	}

	@Test
	void constructor_zeroCourseAndStudentIds_storesZeros() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(0, 0);

		// then — zero is a valid Integer value for course and student refs
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", 0);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", 0);
		assertThat(enrollment.getUuid()).isNotNull();
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
	}

	@Test
	void constructor_negativeIds_storesNegatives() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(-1, -5);

		// then — negative IDs are accepted at the domain level (DB constraints may reject later)
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", -1);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", -5);
		assertThat(enrollment.getUuid()).isNotNull();
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
	}

	@Test
	void constructor_maxIntegerIds_storesMaxValues() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(Integer.MAX_VALUE, Integer.MAX_VALUE);

		// then — boundary values for Integer are accepted
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", Integer.MAX_VALUE);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", Integer.MAX_VALUE);
		assertThat(enrollment.getUuid()).isNotNull();
	}

	@Test
	void constructor_minIntegerIds_storesMinValues() {
		// given / when
		final CourseEnrollment enrollment = new CourseEnrollment(Integer.MIN_VALUE, Integer.MIN_VALUE);

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("course", Integer.MIN_VALUE);
		assertThat(enrollment).hasFieldOrPropertyWithValue("student", Integer.MIN_VALUE);
	}

}
