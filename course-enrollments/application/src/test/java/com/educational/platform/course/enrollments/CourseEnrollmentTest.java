package com.educational.platform.course.enrollments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;
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

	@Test
	void completeLecture_lastActivityMovesForward() {
		// given
		final CourseEnrollmentDTO before = enrollment.toDTO();

		// when
		enrollment.completeLecture(FIRST_LECTURE);

		// then
		final CourseEnrollmentDTO after = enrollment.toDTO();
		assertThat(after.lastActivityAt()).isAfterOrEqualTo(before.lastActivityAt());
		assertThat(after.enrolledAt()).isEqualTo(before.enrolledAt());
		assertThat(after.completedAt()).isNull();
	}

	@Test
	void toDTO_progressPercentRoundedToNearestInteger() {
		// given
		final UUID thirdLecture = UUID.fromString("223e4567-e89b-12d3-a456-426655440003");
		final CourseEnrollment threeLectures = new CourseEnrollment(new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2),
				new CreateCourseCommand.CreateLectureCommand(thirdLecture, "Loops", 3)))),
				new Student(new CreateStudentCommand("username")));

		// when
		threeLectures.completeLecture(FIRST_LECTURE);

		// then
		assertThat(threeLectures.toDTO().progressPercent()).isEqualTo(33);

		// when
		threeLectures.completeLecture(SECOND_LECTURE);

		// then
		assertThat(threeLectures.toDTO().progressPercent()).isEqualTo(67);
		assertThat(threeLectures.toDTO().completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}

	@Test
	void toDTO_referencesCourseAndStudentByNaturalKeys() {
		// when
		final CourseEnrollmentDTO dto = enrollment.toDTO();

		// then
		assertThat(dto.uuid()).isEqualTo(enrollment.getUuid());
		assertThat(dto.course()).isEqualTo(COURSE);
		assertThat(dto.student()).isEqualTo("username");
	}

	@Test
	void toDetailsDTO_lecturesCarryTitleSerialNumberAndCompletion() {
		// given
		enrollment.completeLecture(SECOND_LECTURE);

		// when
		final CourseEnrollmentDetailsDTO details = enrollment.toDetailsDTO();

		// then
		assertThat(details.enrollment()).isEqualTo(enrollment.toDTO());
		assertThat(details.lectures()).containsExactly(
				new LectureProgressDTO(FIRST_LECTURE, "Intro", 1, false),
				new LectureProgressDTO(SECOND_LECTURE, "Variables", 2, true));
	}

	@Test
	void resetLecture_inProgressEnrollment_lectureUnmarkedStatusUnchanged() {
		// given
		enrollment.completeLecture(FIRST_LECTURE);

		// when
		enrollment.resetLecture(FIRST_LECTURE);

		// then
		final CourseEnrollmentDTO dto = enrollment.toDTO();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
		assertThat(dto.completedLectures()).isZero();
		assertThat(dto.progressPercent()).isZero();
		assertThat(dto.completedAt()).isNull();
	}

	@Test
	void resetLecture_completedEnrollmentUncheckedLecture_completionKept() {
		// given
		enrollment.completeLecture(FIRST_LECTURE);
		enrollment.completeLecture(SECOND_LECTURE);
		enrollment.resetLecture(FIRST_LECTURE);
		final CourseEnrollmentDTO reopened = enrollment.toDTO();
		enrollment.completeLecture(FIRST_LECTURE);
		final CourseEnrollmentDTO completed = enrollment.toDTO();

		// when: FIRST_LECTURE is already un-checked and re-checked; resetting a lecture that is not completed is a no-op
		enrollment.resetLecture(FIRST_LECTURE);
		enrollment.completeLecture(FIRST_LECTURE);
		enrollment.resetLecture(SECOND_LECTURE);
		enrollment.completeLecture(SECOND_LECTURE);
		final CourseEnrollmentDTO afterRoundTrip = enrollment.toDTO();

		// then
		assertThat(reopened.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
		assertThat(reopened.completedAt()).isNull();
		assertThat(completed.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(afterRoundTrip.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(afterRoundTrip.completedAt()).isNotNull();
	}

	@Test
	void resetLecture_notCompletedLecture_noOpOnProgress() {
		// given
		enrollment.completeLecture(FIRST_LECTURE);

		// when
		enrollment.resetLecture(SECOND_LECTURE);

		// then
		assertThat(enrollment.toDTO().completedLectures()).isEqualTo(1);
		assertThat(enrollment.toDetailsDTO().lectures()).extracting("completed").containsExactly(true, false);
	}

	@Test
	void resetLecture_unknownLecture_resourceNotFoundException() {
		// given
		final UUID unknown = UUID.randomUUID();

		// when
		final ThrowingCallable resetAction = () -> enrollment.resetLecture(unknown);

		// then
		assertThatThrownBy(resetAction)
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(unknown.toString());
	}

	@Test
	void resetLecture_archivedEnrollment_archivedException() {
		// given
		enrollment.completeLecture(FIRST_LECTURE);
		enrollment.archive();

		// when
		final ThrowingCallable resetAction = () -> enrollment.resetLecture(FIRST_LECTURE);

		// then
		assertThatThrownBy(resetAction)
				.isInstanceOf(CourseEnrollmentArchivedException.class)
				.isInstanceOf(UnprocessableEntityException.class)
				.hasMessageContaining(enrollment.getUuid().toString());
		assertThat(enrollment.toDTO().completedLectures()).isEqualTo(1);
	}

	@Test
	void archive_completedEnrollment_completionStatusKept() {
		// given
		enrollment.completeLecture(FIRST_LECTURE);
		enrollment.completeLecture(SECOND_LECTURE);
		final LocalDateTime completedAt = enrollment.toDTO().completedAt();

		// when
		enrollment.archive();

		// then
		final CourseEnrollmentDTO archived = enrollment.toDTO();
		assertThat(archived.archived()).isTrue();
		assertThat(archived.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(archived.completedAt()).isEqualTo(completedAt);
		assertThat(archived.progressPercent()).isEqualTo(100);

		// when
		enrollment.restore();

		// then
		final CourseEnrollmentDTO restored = enrollment.toDTO();
		assertThat(restored.archived()).isFalse();
		assertThat(restored.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(restored.completedAt()).isEqualTo(completedAt);
	}

	@Test
	void archive_alreadyArchived_idempotent() {
		// given
		enrollment.archive();
		final Object archivedAt = ReflectionTestUtils.getField(enrollment, "archivedAt");
		final LocalDateTime lastActivityAt = enrollment.toDTO().lastActivityAt();
		assertThat(archivedAt).isNotNull();

		// when
		enrollment.archive();

		// then
		assertThat(enrollment.toDTO().archived()).isTrue();
		assertThat(ReflectionTestUtils.getField(enrollment, "archivedAt")).isSameAs(archivedAt);
		assertThat(enrollment.toDTO().lastActivityAt()).isEqualTo(lastActivityAt);
	}

	@Test
	void restore_notArchived_noOp() {
		// given
		final LocalDateTime lastActivityAt = enrollment.toDTO().lastActivityAt();

		// when
		enrollment.restore();

		// then
		assertThat(enrollment.toDTO().archived()).isFalse();
		assertThat(enrollment.toDTO().lastActivityAt()).isEqualTo(lastActivityAt);
		assertThat(ReflectionTestUtils.getField(enrollment, "archivedAt")).isNull();
	}

	@Test
	void restore_archived_archivedAtClearedAndActivityRecorded() {
		// given
		enrollment.archive();
		final LocalDateTime archivedAt = enrollment.toDTO().lastActivityAt();

		// when
		enrollment.restore();

		// then
		assertThat(ReflectionTestUtils.getField(enrollment, "archivedAt")).isNull();
		assertThat(enrollment.toDTO().lastActivityAt()).isAfterOrEqualTo(archivedAt);
	}

	@Test
	void toDTO_courseRefreshedWithoutCompletedLecture_progressCountsCurrentLecturesOnly() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2))));
		final CourseEnrollment sut = new CourseEnrollment(course, new Student(new CreateStudentCommand("username")));
		sut.completeLecture(FIRST_LECTURE);

		// when
		course.refresh(new CreateCourseCommand(COURSE, "Java Basics v2", List.of(
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 1))));

		// then
		final CourseEnrollmentDTO dto = sut.toDTO();
		assertThat(dto.courseName()).isEqualTo("Java Basics v2");
		assertThat(dto.totalLectures()).isEqualTo(1);
		assertThat(dto.completedLectures()).isZero();
		assertThat(dto.progressPercent()).isZero();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);

		// when
		sut.completeLecture(SECOND_LECTURE);

		// then
		assertThat(sut.toDTO().completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(sut.toDTO().progressPercent()).isEqualTo(100);
	}

	@Test
	void completeLecture_completedCourseGainsLectureOnRepublish_statusAndCompletedAtKept() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2))));
		final CourseEnrollment sut = new CourseEnrollment(course, new Student(new CreateStudentCommand("username")));
		sut.completeLecture(FIRST_LECTURE);
		sut.completeLecture(SECOND_LECTURE);
		final LocalDateTime completedAt = sut.toDTO().completedAt();
		assertThat(completedAt).isNotNull();

		// when
		final UUID thirdLecture = UUID.fromString("223e4567-e89b-12d3-a456-426655440003");
		course.refresh(new CreateCourseCommand(COURSE, "Java Basics", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
				new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2),
				new CreateCourseCommand.CreateLectureCommand(thirdLecture, "Loops", 3))));

		// then
		CourseEnrollmentDTO dto = sut.toDTO();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(dto.completedLectures()).isEqualTo(2);
		assertThat(dto.totalLectures()).isEqualTo(3);
		assertThat(dto.progressPercent()).isEqualTo(67);
		assertThat(dto.completedAt()).isEqualTo(completedAt);

		// when
		sut.completeLecture(thirdLecture);

		// then
		dto = sut.toDTO();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
		assertThat(dto.progressPercent()).isEqualTo(100);
		assertThat(dto.completedAt()).isEqualTo(completedAt);
		assertThat(dto.lastActivityAt()).isAfterOrEqualTo(completedAt);
	}

}
