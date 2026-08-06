package com.educational.platform.course.enrollments.register;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.enrollments.student.Student;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RegisterStudentToCourseCommandHandlerTest {

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
        sut = new RegisterStudentToCourseCommandHandler(transactionTemplate, courseEnrollmentRepository, courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_validCommand_enrollmentSavedAndUuidReturned() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);

        final Student student = mock(Student.class);
        when(student.toReference()).thenReturn("username");
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID uuid = sut.handle(command);

        // then
        verify(courseEnrollmentRepository).save(enrollment);
        assertThat(uuid).isEqualTo(enrollment.getUuid());

        final InOrder inOrder = inOrder(transactionManager, eventPublisher);
        inOrder.verify(transactionManager).commit(any());
        inOrder.verify(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

    @Test
    void handle_validCommand_enrollmentSavedWithinTransaction() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);

        final Student student = mock(Student.class);
        when(student.toReference()).thenReturn("username");
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        final InOrder inOrder = inOrder(transactionManager, courseEnrollmentRepository);
        inOrder.verify(transactionManager).getTransaction(any());
        inOrder.verify(courseEnrollmentRepository).save(enrollment);
        inOrder.verify(transactionManager).commit(any());
    }

    @Test
    void handle_validCommand_studentEnrolledToCourseIntegrationEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);

        final Student student = mock(Student.class);
        when(student.toReference()).thenReturn("username");
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> argument = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("courseId", courseId)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handle_enrollmentCreationFails_transactionRolledBackAndNoEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final RuntimeException failure = new RuntimeException("enrollment creation failed");
        when(courseEnrollmentFactory.createFrom(command)).thenThrow(failure);

        // when
        final Throwable thrown = catchThrowable(() -> sut.handle(command));

        // then
        assertThat(thrown).isSameAs(failure);
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(courseEnrollmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

}
