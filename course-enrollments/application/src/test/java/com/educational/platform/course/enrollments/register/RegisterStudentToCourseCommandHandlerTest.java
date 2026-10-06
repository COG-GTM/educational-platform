package com.educational.platform.course.enrollments.register;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins the idempotent registration contract: a repeated enrollment returns the existing enrollment and does not publish a second event.
 */
@ExtendWith(MockitoExtension.class)
public class RegisterStudentToCourseCommandHandlerTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private CourseEnrollmentRepository repository;

    @Mock
    private CourseEnrollmentFactory factory;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Student student;
    private EnrollCourse course;
    private CourseEnrollment enrollment;

    private RegisterStudentToCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        student = new Student(new CreateStudentCommand("username"));
        course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(LECTURE, "Intro", 1))));
        enrollment = new CourseEnrollment(course, student);
        sut = new RegisterStudentToCourseCommandHandler(new TransactionTemplate(transactionManager), repository, factory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_notEnrolledYet_enrollmentSavedAndEventPublished() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(COURSE);
        when(factory.createFrom(command)).thenReturn(enrollment);
        when(repository.findFirstByCourseAndStudentOrderByIdAsc(course, student)).thenReturn(Optional.empty());
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(enrollment.getUuid());
        verify(repository).save(enrollment);
        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> event = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().courseId()).isEqualTo(COURSE);
        assertThat(event.getValue().username()).isEqualTo("username");
    }

    @Test
    void handle_alreadyEnrolled_existingUuidReturnedNothingSavedNoEvent() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(COURSE);
        final CourseEnrollment existing = new CourseEnrollment(course, student);
        existing.completeLecture(LECTURE);
        when(factory.createFrom(command)).thenReturn(enrollment);
        when(repository.findFirstByCourseAndStudentOrderByIdAsc(course, student)).thenReturn(Optional.of(existing));

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(existing.getUuid()).isNotEqualTo(enrollment.getUuid());
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_notEnrolledYet_eventPublishedOnlyAfterTransactionCommitted() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(COURSE);
        when(factory.createFrom(command)).thenReturn(enrollment);
        when(repository.findFirstByCourseAndStudentOrderByIdAsc(course, student)).thenReturn(Optional.empty());
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        final InOrder inOrder = inOrder(transactionManager, repository, eventPublisher);
        inOrder.verify(repository).save(enrollment);
        inOrder.verify(transactionManager).commit(any());
        inOrder.verify(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

    @Test
    void handle_courseCannotBeResolved_transactionRolledBackNothingSavedNoEvent() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(COURSE);
        when(factory.createFrom(command)).thenThrow(new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + COURSE));

        // when
        final ThrowingCallable registerAction = () -> sut.handle(command);

        // then
        assertThatThrownBy(registerAction)
                .isInstanceOf(RelatedResourceIsNotResolvedException.class)
                .hasMessageContaining(COURSE.toString());
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
