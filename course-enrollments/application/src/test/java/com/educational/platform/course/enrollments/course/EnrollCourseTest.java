package com.educational.platform.course.enrollments.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

public class EnrollCourseTest {

	private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
	private static final UUID THIRD_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440003");

	@Test
	void create_lecturesCopied() {
		// when
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2))));

		// then
		assertThat(course.getName()).isEqualTo("Java Basics");
		assertThat(course.getLectures()).extracting("title").containsExactly("Intro", "Variables");
		assertThat(course.lectureByUuid(SECOND_LECTURE)).isPresent();
	}

	@Test
	void refresh_republishedCourse_knownLecturesKeptNewAddedRemovedDropped() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2))));
		final EnrollLecture first = course.lectureByUuid(FIRST_LECTURE).orElseThrow();

		// when
		course.refresh(new CreateCourseCommand(COURSE, "Java Basics v2", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(THIRD_LECTURE, "Loops", 2))));

		// then
		assertThat(course.getName()).isEqualTo("Java Basics v2");
		assertThat(course.getLectures()).extracting("uuid").containsExactly(FIRST_LECTURE, THIRD_LECTURE);
		assertThat(course.lectureByUuid(FIRST_LECTURE)).containsSame(first);
	}

	@Test
	void refresh_republishedLectureKeepsUuid_titleAndSerialNumberUpdated() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2))));
		final EnrollLecture first = course.lectureByUuid(FIRST_LECTURE).orElseThrow();

		// when
		course.refresh(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 1),
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Welcome", 2))));

		// then
		assertThat(course.lectureByUuid(FIRST_LECTURE)).containsSame(first);
		assertThat(first.getTitle()).isEqualTo("Welcome");
		assertThat(first.getSerialNumber()).isEqualTo(2);
		assertThat(course.lectureByUuid(SECOND_LECTURE).orElseThrow().getSerialNumber()).isEqualTo(1);
	}

	@Test
	void create_uuidOnlyCommand_noNameAndNoLectures() {
		// when
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE));

		// then
		assertThat(course.toReference()).isEqualTo(COURSE);
		assertThat(course.getName()).isNull();
		assertThat(course.getLectures()).isEmpty();
		assertThat(course.lectureByUuid(FIRST_LECTURE)).isEmpty();
	}

	@Test
	void create_lecturesKeepTitleSerialNumberAndCourse() {
		// when
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1))));

		// then
		final EnrollLecture lecture = course.lectureByUuid(FIRST_LECTURE).orElseThrow();
		assertThat(lecture.toReference()).isEqualTo(FIRST_LECTURE);
		assertThat(lecture.getTitle()).isEqualTo("Intro");
		assertThat(lecture.getSerialNumber()).isEqualTo(1);
		assertThat(lecture).hasFieldOrPropertyWithValue("course", course);
	}

	@Test
	void refresh_nullLectures_allLecturesDropped() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1))));

		// when
		course.refresh(new CreateCourseCommand(COURSE, "Java Basics", null));

		// then
		assertThat(course.getLectures()).isEmpty();
		assertThat(course.getName()).isEqualTo("Java Basics");
	}

	@Test
	void refresh_sameSnapshotTwice_noDuplicates() {
		// given
		final CreateCourseCommand snapshot = new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2)));
		final EnrollCourse course = new EnrollCourse(snapshot);

		// when
		final EnrollCourse result = course.refresh(snapshot);

		// then
		assertThat(result).isSameAs(course);
		assertThat(course.getLectures()).extracting("uuid").containsExactly(FIRST_LECTURE, SECOND_LECTURE);
	}

	@Test
	void getLectures_unmodifiable() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1))));

		// when
		final ThrowingCallable clearAction = () -> course.getLectures().clear();

		// then
		assertThatThrownBy(clearAction).isInstanceOf(UnsupportedOperationException.class);
		assertThat(course.getLectures()).hasSize(1);
	}

	@Test
	void lectureByUuid_unknownLecture_empty() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1))));

		// when / then
		assertThat(course.lectureByUuid(THIRD_LECTURE)).isEmpty();
	}
}
