package com.educational.platform.course.enrollments.progress;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentArchivedException;
import com.educational.platform.course.enrollments.CourseEnrollmentDetailsDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateLectureProgressCommandHandlerTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
    private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

    @Mock
    private CourseEnrollmentRepository repository;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    private Student student;
    private CourseEnrollment enrollment;

    private UpdateLectureProgressCommandHandler sut;

    @BeforeEach
    void setUp() {
        student = new Student(new CreateStudentCommand("username"));
        enrollment = new CourseEnrollment(new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
                new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2)))), student);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        sut = new UpdateLectureProgressCommandHandler(repository, currentUserAsStudent);
    }

    @Test
    void handle_completed_lectureMarkedSavedAndDetailsReturned() {
        // given
        when(repository.findByUuidAndStudent(enrollment.getUuid(), student)).thenReturn(Optional.of(enrollment));

        // when
        final CourseEnrollmentDetailsDTO result = sut.handle(new UpdateLectureProgressCommand(enrollment.getUuid(), FIRST_LECTURE, true));

        // then
        verify(repository).save(enrollment);
        assertThat(result.enrollment().completedLectures()).isEqualTo(1);
        assertThat(result.enrollment().progressPercent()).isEqualTo(50);
        assertThat(result.enrollment().completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
        assertThat(result.lectures()).extracting("uuid", "completed")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(FIRST_LECTURE, true), org.assertj.core.groups.Tuple.tuple(SECOND_LECTURE, false));
    }

    @Test
    void handle_notCompleted_lectureResetAndSaved() {
        // given
        enrollment.completeLecture(FIRST_LECTURE);
        enrollment.completeLecture(SECOND_LECTURE);
        when(repository.findByUuidAndStudent(enrollment.getUuid(), student)).thenReturn(Optional.of(enrollment));

        // when
        final CourseEnrollmentDetailsDTO result = sut.handle(new UpdateLectureProgressCommand(enrollment.getUuid(), FIRST_LECTURE, false));

        // then
        verify(repository).save(enrollment);
        assertThat(result.enrollment().completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
        assertThat(result.enrollment().completedAt()).isNull();
        assertThat(result.lectures()).extracting("completed").containsExactly(false, true);
    }

    @Test
    void handle_unknownEnrollment_resourceNotFoundExceptionAndNothingSaved() {
        // given
        final UUID unknown = UUID.randomUUID();
        when(repository.findByUuidAndStudent(unknown, student)).thenReturn(Optional.empty());

        // when
        final ThrowingCallable action = () -> sut.handle(new UpdateLectureProgressCommand(unknown, FIRST_LECTURE, true));

        // then
        assertThatThrownBy(action)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknown.toString());
        verify(repository, never()).save(any());
    }

    @Test
    void handle_lectureNotInCourse_resourceNotFoundExceptionAndNothingSaved() {
        // given
        final UUID foreignLecture = UUID.randomUUID();
        when(repository.findByUuidAndStudent(enrollment.getUuid(), student)).thenReturn(Optional.of(enrollment));

        // when
        final ThrowingCallable action = () -> sut.handle(new UpdateLectureProgressCommand(enrollment.getUuid(), foreignLecture, true));

        // then
        assertThatThrownBy(action)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(foreignLecture.toString());
        verify(repository, never()).save(any());
        assertThat(enrollment.toDTO().completedLectures()).isZero();
    }

    @Test
    void handle_archivedEnrollment_archivedExceptionAndNothingSaved() {
        // given
        enrollment.archive();
        when(repository.findByUuidAndStudent(enrollment.getUuid(), student)).thenReturn(Optional.of(enrollment));

        // when
        final ThrowingCallable action = () -> sut.handle(new UpdateLectureProgressCommand(enrollment.getUuid(), FIRST_LECTURE, false));

        // then
        assertThatThrownBy(action).isInstanceOf(CourseEnrollmentArchivedException.class);
        verify(repository, never()).save(any());
    }
}
