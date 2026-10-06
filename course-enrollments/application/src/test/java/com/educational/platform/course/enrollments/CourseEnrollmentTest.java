package com.educational.platform.course.enrollments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

public class CourseEnrollmentTest {

	private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

	private CourseEnrollment enrollment;

	@BeforeEach
	void setUp() {
		final CreateCourseCommand createCourseCommand = new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2)));
		final CreateStudentCommand createStudentCommand = new CreateStudentCommand("username");
		enrollment = new CourseEnrollment(new EnrollCourse(createCourseCommand), new Student(createStudentCommand));
	}

	@Test
	void complete_completedStatus() {
		// when
		enrollment.complete();

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
		assertThat(enrollment.toDTO().completedAt()).isNotNull();
	}

	@Test
	void create_inProgressWithoutCompletedLectures() {
		// when
		final CourseEnrollmentDTO dto = enrollment.toDTO();

		// then
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
		assertThat(dto.courseName()).isEqualTo("Java Basics");
		assertThat(dto.totalLectures()).isEqualTo(2);
		assertThat(dto.completedLectures()).isZero();
		assertThat(dto.progressPercent()).isZero();
		assertThat(dto.archived()).isFalse();
		assertThat(dto.enrolledAt()).isNotNull();
		assertThat(dto.lastActivityAt()).isEqualTo(dto.enrolledAt());
	}

	@Test
	void completeLecture_oneOfTwo_progressHalfAndStillInProgress() {
		// when
		enrollment.completeLecture(FIRST_LECTURE);

		// then
		final CourseEnrollmentDTO dto = enrollment.toDTO();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
		assertThat(dto.completedLectures()).isEqualTo(1);
		assertThat(dto.progressPercent()).isEqualTo(50);
		assertThat(enrollment.toDetailsDTO().lectures()).extracting("completed").containsExactly(true, false);
	}

	@Test
	void completeLecture_allLectures_enrollmentCompleted() {
		// when
		enrollment.completeLecture(FIRST_LECTURE);
		enrollment.completeLecture(SECOND_LECTURE);

		// then
		final CourseEnrollmentDTO dto = enrollment.toDTO();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(dto.progressPercent()).isEqualTo(100);
		assertThat(dto.completedAt()).isNotNull();
	}

	@Test
	void completeLecture_twice_idempotent() {
		// when
		enrollment.completeLecture(FIRST_LECTURE);
		enrollment.completeLecture(FIRST_LECTURE);

		// then
		assertThat(enrollment.toDTO().completedLectures()).isEqualTo(1);
	}

	@Test
	void resetLecture_completedEnrollment_backToInProgress() {
		// given
		enrollment.completeLecture(FIRST_LECTURE);
		enrollment.completeLecture(SECOND_LECTURE);

		// when
		enrollment.resetLecture(SECOND_LECTURE);

		// then
		final CourseEnrollmentDTO dto = enrollment.toDTO();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
		assertThat(dto.completedLectures()).isEqualTo(1);
		assertThat(dto.completedAt()).isNull();
	}

	@Test
	void completeLecture_unknownLecture_resourceNotFoundException() {
		assertThatThrownBy(() -> enrollment.completeLecture(UUID.randomUUID()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void completeLecture_courseWithoutLectures_staysInProgress() {
		// given
		final CourseEnrollment empty = new CourseEnrollment(new EnrollCourse(new CreateCourseCommand(COURSE)),
				new Student(new CreateStudentCommand("username")));

		// then
		assertThat(empty.toDTO().progressPercent()).isZero();
		assertThat(empty.toDTO().completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}

	@Test
	void archive_thenRestore_progressKept() {
		// given
		enrollment.completeLecture(FIRST_LECTURE);

		// when
		enrollment.archive();

		// then
		assertThat(enrollment.toDTO().archived()).isTrue();
		assertThatThrownBy(() -> enrollment.completeLecture(SECOND_LECTURE))
				.isInstanceOf(CourseEnrollmentArchivedException.class);

		// when
		enrollment.restore();

		// then
		final CourseEnrollmentDTO dto = enrollment.toDTO();
		assertThat(dto.archived()).isFalse();
		assertThat(dto.completedLectures()).isEqualTo(1);
	}

}
