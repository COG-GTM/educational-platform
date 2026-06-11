package com.educational.platform.course.enrollments.course;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

import org.junit.jupiter.api.Test;

import com.educational.platform.common.domain.AggregateRoot;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollCourseTest {

	@Test
	void constructor_validCommand_storesUuid() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final CreateCourseCommand command = new CreateCourseCommand(uuid);

		// when
		final EnrollCourse course = new EnrollCourse(command);

		// then
		assertThat(course.toReference()).isEqualTo(uuid);
	}

	@Test
	void constructor_nullUuid_storesNull() {
		// given
		final CreateCourseCommand command = new CreateCourseCommand(null);

		// when
		final EnrollCourse course = new EnrollCourse(command);

		// then
		assertThat(course.toReference()).isNull();
	}

	@Test
	void getId_beforePersist_returnsNull() {
		// given
		final CreateCourseCommand command = new CreateCourseCommand(UUID.randomUUID());

		// when
		final EnrollCourse course = new EnrollCourse(command);

		// then
		assertThat(course.getId()).isNull();
	}

	@Test
	void toReference_returnsUuidFromCommand() {
		// given
		final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(uuid));

		// when
		final UUID reference = course.toReference();

		// then
		assertThat(reference).isEqualTo(uuid);
	}

	@Test
	void constructor_differentUuids_produceDifferentReferences() {
		// given
		final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

		// when
		final EnrollCourse course1 = new EnrollCourse(new CreateCourseCommand(uuid1));
		final EnrollCourse course2 = new EnrollCourse(new CreateCourseCommand(uuid2));

		// then
		assertThat(course1.toReference()).isNotEqualTo(course2.toReference());
	}

	@Test
	void toReference_multipleCalls_returnsSameValue() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(uuid));

		// when
		final UUID ref1 = course.toReference();
		final UUID ref2 = course.toReference();

		// then
		assertThat(ref1).isEqualTo(ref2);
	}

	@Test
	void enrollCourse_implementsAggregateRoot() {
		// given / when
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(UUID.randomUUID()));

		// then
		assertThat(course).isInstanceOf(AggregateRoot.class);
	}
}
