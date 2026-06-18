package com.educational.platform.course.enrollments.register;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStudentToCourseCommandHandlerTest {

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private CourseEnrollmentFactory courseEnrollmentFactory;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegisterStudentToCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        sut = new RegisterStudentToCourseCommandHandler(transactionTemplate, courseEnrollmentRepository,
                courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_validCommand_studentEnrolledToCourseIntegrationEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);
        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        verify(courseEnrollmentRepository).save(enrollment);
        assertThat(result).isEqualTo(enrollment.getUuid());

        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> argument =
                ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("courseId", courseId)
                .hasFieldOrPropertyWithValue("username", "student");
    }

    @Test
    void handle_courseCannotBeResolved_exceptionPropagatedAndNoEventPublished() {
        // given - enrollment fails inside the transaction, so the integration event must not be published
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(courseEnrollmentFactory.createFrom(command))
                .thenThrow(new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + courseId));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_repositoryThrows_exceptionPropagatedAndNoEventPublished() {
        // given - the enrollment is built but persistence fails inside the transaction, so the event
        // published only after a successful write must not be emitted
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);
        doThrow(new RuntimeException("db error")).when(courseEnrollmentRepository).save(enrollment);

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle);
        verify(eventPublisher, never()).publishEvent(any());
    }

}
