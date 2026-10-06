package com.educational.platform.course.enrollments.archive;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
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
public class RestoreCourseEnrollmentCommandHandlerTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseEnrollmentRepository repository;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    private Student student;
    private CourseEnrollment enrollment;

    private RestoreCourseEnrollmentCommandHandler sut;

    @BeforeEach
    void setUp() {
        student = new Student(new CreateStudentCommand("username"));
        enrollment = new CourseEnrollment(new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(LECTURE, "Intro", 1)))), student);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        sut = new RestoreCourseEnrollmentCommandHandler(repository, currentUserAsStudent);
    }

    @Test
    void handle_archivedEnrollment_restoredWithProgressSavedAndReturned() {
        // given
        enrollment.completeLecture(LECTURE);
        enrollment.archive();
        when(repository.findByUuidAndStudent(enrollment.getUuid(), student)).thenReturn(Optional.of(enrollment));

        // when
        final CourseEnrollmentDTO result = sut.handle(new RestoreCourseEnrollmentCommand(enrollment.getUuid()));

        // then
        verify(repository).save(enrollment);
        assertThat(result.uuid()).isEqualTo(enrollment.getUuid());
        assertThat(result.archived()).isFalse();
        assertThat(result.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
        assertThat(result.completedLectures()).isEqualTo(1);
        assertThat(result.progressPercent()).isEqualTo(100);
    }

    @Test
    void handle_notArchived_stillActiveAndSaved() {
        // given
        when(repository.findByUuidAndStudent(enrollment.getUuid(), student)).thenReturn(Optional.of(enrollment));

        // when
        final CourseEnrollmentDTO result = sut.handle(new RestoreCourseEnrollmentCommand(enrollment.getUuid()));

        // then
        assertThat(result.archived()).isFalse();
        verify(repository).save(enrollment);
    }

    @Test
    void handle_enrollmentNotOwnedOrUnknown_resourceNotFoundExceptionAndNothingSaved() {
        // given
        final UUID unknown = UUID.randomUUID();
        when(repository.findByUuidAndStudent(unknown, student)).thenReturn(Optional.empty());

        // when
        final ThrowingCallable restoreAction = () -> sut.handle(new RestoreCourseEnrollmentCommand(unknown));

        // then
        assertThatThrownBy(restoreAction)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknown.toString());
        verify(repository, never()).save(any());
    }
}
