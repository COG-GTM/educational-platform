package com.educational.platform.course.enrollments.register;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class RegisterStudentToCourseCommandHandlerTest {

    @Mock
    private TransactionTemplate transactionTemplate;

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
        sut = new RegisterStudentToCourseCommandHandler(transactionTemplate, courseEnrollmentRepository, courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_validCommand_enrollmentCreatedAndEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(courseEnrollment.getUuid());
        verify(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

    @Test
    void handle_validCommand_integrationEventContainsCorrectData() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> eventCaptor = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        final StudentEnrolledToCourseIntegrationEvent event = eventCaptor.getValue();
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.username()).isEqualTo("student");
    }

    @Test
    void handle_transactionReturnsNull_throwsNullPointerException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        when(transactionTemplate.execute(any())).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(NullPointerException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_currentUserReturnsNull_throwsNullPointerException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);
        when(currentUserAsStudent.userAsStudent()).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handle_validCommand_returnsEnrollmentUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);
        final UUID expectedUuid = courseEnrollment.getUuid();

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedUuid);
    }

    @Test
    void handle_validCommand_transactionTemplateIsInvoked() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        verify(transactionTemplate).execute(any());
    }

    @Test
    void handle_validCommand_eventPublishedWithCorrectCourseId() {
        // given
        final UUID courseId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("enrolled-student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> eventCaptor = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        final StudentEnrolledToCourseIntegrationEvent event = eventCaptor.getValue();
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.username()).isEqualTo("enrolled-student");
    }

    @Test
    void handle_multipleInvocations_publishMultipleEvents() {
        // given
        final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseEnrollment enrollment1 = new CourseEnrollment(1, 1);
        final CourseEnrollment enrollment2 = new CourseEnrollment(2, 1);

        when(transactionTemplate.execute(any())).thenReturn(enrollment1, enrollment2);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(new RegisterStudentToCourseCommand(courseId1));
        sut.handle(new RegisterStudentToCourseCommand(courseId2));

        // then
        verify(eventPublisher, times(2)).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handle_validCommand_transactionCallbackCreatesAndSavesEnrollment() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment expectedEnrollment = new CourseEnrollment(1, 1);

        when(courseEnrollmentFactory.createFrom(command)).thenReturn(expectedEnrollment);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        verify(courseEnrollmentFactory).createFrom(command);
        verify(courseEnrollmentRepository).save(expectedEnrollment);
    }

    @SuppressWarnings("unchecked")
    @Test
    void handle_validCommand_transactionCallbackReturnsCreatedEnrollment() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment expectedEnrollment = new CourseEnrollment(1, 1);

        when(courseEnrollmentFactory.createFrom(command)).thenReturn(expectedEnrollment);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(expectedEnrollment.getUuid());
    }

    @Test
    void handle_transactionCallbackThrows_eventNotPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("transaction failed"));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("transaction failed");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_validCommand_currentUserCalledForEventUsername() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("event-student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then — currentUserAsStudent is called to resolve the username for the event
        verify(currentUserAsStudent).userAsStudent();
        ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> captor = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("event-student");
    }

    @Test
    void handle_validCommand_returnedUuidIsNotNull() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.toString()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void handle_eventPublisherThrows_exceptionPropagates() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        doThrow(new RuntimeException("event publish failed"))
                .when(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("event publish failed");
    }

    @Test
    void handle_validCommand_doesNotInteractWithRepositoryDirectly() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then — handler delegates persistence to transactionTemplate callback, not directly
        verifyNoInteractions(courseEnrollmentRepository);
    }

    @Test
    void handle_validCommand_doesNotInteractWithFactoryDirectly() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then — factory is called inside the transaction callback, not directly by handler
        verifyNoInteractions(courseEnrollmentFactory);
    }

    @SuppressWarnings("unchecked")
    @Test
    void handle_validCommand_factoryCalledBeforeSaveInTransactionCallback() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment expectedEnrollment = new CourseEnrollment(1, 1);

        when(courseEnrollmentFactory.createFrom(command)).thenReturn(expectedEnrollment);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then — factory must be called before save within the transaction
        org.mockito.InOrder inOrder = inOrder(courseEnrollmentFactory, courseEnrollmentRepository);
        inOrder.verify(courseEnrollmentFactory).createFrom(command);
        inOrder.verify(courseEnrollmentRepository).save(expectedEnrollment);
    }

    @SuppressWarnings("unchecked")
    @Test
    void handle_validCommand_eventPublishedAfterTransaction() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment expectedEnrollment = new CourseEnrollment(1, 1);

        when(courseEnrollmentFactory.createFrom(command)).thenReturn(expectedEnrollment);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then — event published after transaction template executes
        org.mockito.InOrder inOrder = inOrder(transactionTemplate, eventPublisher);
        inOrder.verify(transactionTemplate).execute(any(TransactionCallback.class));
        inOrder.verify(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

    @Test
    void handle_studentWithNullUsername_eventPublishedWithNullUsername() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand(null));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then — event is published with null username since student.toReference() returns null
        ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> captor = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().username()).isNull();
        assertThat(captor.getValue().courseId()).isEqualTo(courseId);
    }

    @Test
    void handle_nullCommand_throwsNullPointerException() {
        // given
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);
        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        // when / then — command.courseId() throws NPE when creating the integration event
        assertThatThrownBy(() -> sut.handle(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handle_differentStudents_eventContainsEachStudentUsername() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment enrollment1 = new CourseEnrollment(1, 1);
        final CourseEnrollment enrollment2 = new CourseEnrollment(1, 2);

        when(transactionTemplate.execute(any())).thenReturn(enrollment1, enrollment2);

        final Student student1 = new Student(new CreateStudentCommand("alice"));
        final Student student2 = new Student(new CreateStudentCommand("bob"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student1, student2);

        // when
        sut.handle(command);
        sut.handle(command);

        // then — each event has the corresponding student's username
        ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> captor = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues().get(0).username()).isEqualTo("alice");
        assertThat(captor.getAllValues().get(1).username()).isEqualTo("bob");
    }

    @Test
    void handle_multipleCommands_returnDistinctUuids() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment enrollment1 = new CourseEnrollment(1, 1);
        final CourseEnrollment enrollment2 = new CourseEnrollment(1, 2);

        when(transactionTemplate.execute(any())).thenReturn(enrollment1, enrollment2);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID uuid1 = sut.handle(command);
        final UUID uuid2 = sut.handle(command);

        // then — each invocation returns a distinct enrollment UUID
        assertThat(uuid1).isNotNull();
        assertThat(uuid2).isNotNull();
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void handle_factoryThrowsInsideCallback_saveNotCalled() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        when(courseEnrollmentFactory.createFrom(command)).thenThrow(new RuntimeException("factory error"));
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("factory error");
        verify(courseEnrollmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void handle_eventPublisherThrows_enrollmentAlreadySaved() {
        // given — event publishing fails AFTER the transaction callback succeeds
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment expectedEnrollment = new CourseEnrollment(1, 1);

        when(courseEnrollmentFactory.createFrom(command)).thenReturn(expectedEnrollment);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        doThrow(new RuntimeException("event publish failed"))
                .when(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));

        // when / then — exception propagates but save was already called inside the transaction
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("event publish failed");
        verify(courseEnrollmentRepository).save(expectedEnrollment);
    }

    @SuppressWarnings("unchecked")
    @Test
    void handle_saveThrowsInsideCallback_exceptionPropagatesAndEventNotPublished() {
        // given — save fails inside the transaction callback (e.g., unique constraint violation)
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment expectedEnrollment = new CourseEnrollment(1, 1);

        when(courseEnrollmentFactory.createFrom(command)).thenReturn(expectedEnrollment);
        doThrow(new RuntimeException("unique constraint violation"))
                .when(courseEnrollmentRepository).save(any());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        // when / then — save exception propagates, event is never published
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unique constraint violation");
        verify(courseEnrollmentFactory).createFrom(command);
        verify(courseEnrollmentRepository).save(expectedEnrollment);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
