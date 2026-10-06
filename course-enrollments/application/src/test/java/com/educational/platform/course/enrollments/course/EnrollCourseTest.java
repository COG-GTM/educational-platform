package com.educational.platform.course.enrollments.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

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
}
