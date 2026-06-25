package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class StudentEnrolledToCourseIntegrationEventHandlerTest {

    @Mock
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedEventRepository;

    @InjectMocks
    private StudentEnrolledToCourseIntegrationEventHandler sut;

    @Test
    void handleStudentEnrolledToCourseEvent_updateNumberOfStudentsCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> argument = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(argument.capture());
        final IncreaseNumberOfStudentsCommand updateNumberOfStudentsCommand = argument.getValue();
        assertThat(updateNumberOfStudentsCommand)
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_transientException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_businessException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_pessimisticLockingException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new PessimisticLockingFailureException("pessimistic lock"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(PessimisticLockingFailureException.class);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_successfulHandling_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("DB connection lost");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(failedEvent.getEventPayload()).isEqualTo(event.toString());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("DB connection lost");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void recover_withNullExceptionMessage_usesExceptionClassName() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException(null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage()).isEqualTo(OptimisticLockingFailureException.class.getName());
    }

    @Test
    void recover_withPessimisticLockingException_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final PessimisticLockingFailureException exception = new PessimisticLockingFailureException("deadlock detected");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("deadlock detected");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_genericRuntimeException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new IllegalStateException("invalid state"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("invalid state");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_transientException_preservesOriginalMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new OptimisticLockingFailureException("specific DB error"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("specific DB error");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_transientException_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try {
            sut.handleStudentEnrolledToCourseEvent(event);
        } catch (OptimisticLockingFailureException ignored) {
        }

        // then
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void handlerMethod_hasRetryableAnnotationWithCorrectConfig() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        // then
        assertThat(retryable).isNotNull();
        assertThat(retryable.retryFor()).containsExactlyInAnyOrder(
                TransientDataAccessException.class,
                OptimisticLockingFailureException.class,
                PessimisticLockingFailureException.class
        );
        assertThat(retryable.maxAttempts()).isEqualTo(3);
        assertThat(retryable.backoff().delay()).isEqualTo(500);
        assertThat(retryable.backoff().multiplier()).isEqualTo(2);
    }

    @Test
    void handlerMethod_hasAsyncAndEventListenerAnnotations() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.isAnnotationPresent(Async.class)).isTrue();
        assertThat(method.isAnnotationPresent(EventListener.class)).isTrue();
    }

    @Test
    void recoverMethod_hasRecoverAnnotationWithCorrectParameterTypes() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.isAnnotationPresent(Recover.class)).isTrue();
    }

    @Test
    void class_hasComponentAnnotation() {
        // then
        assertThat(StudentEnrolledToCourseIntegrationEventHandler.class.isAnnotationPresent(Component.class)).isTrue();
    }

    @Test
    void recover_withEmptyExceptionMessage_persistsEmptyMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage()).isEmpty();
    }

    @Test
    void recover_withTransientDataAccessSubclass_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final QueryTimeoutException exception = new QueryTimeoutException("query timed out");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("query timed out");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_businessException_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try {
            sut.handleStudentEnrolledToCourseEvent(event);
        } catch (ResourceNotFoundException ignored) {
        }

        // then
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_queryTimeoutException_rethrown() {
        // given - QueryTimeoutException is a TransientDataAccessException subclass
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new QueryTimeoutException("query timed out"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(QueryTimeoutException.class)
                .hasMessage("query timed out");
    }

    @Test
    void recover_withQueryTimeoutException_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final QueryTimeoutException exception = new QueryTimeoutException("query timed out");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("query timed out");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
    }

    @Test
    void recover_eventPayloadContainsCourseIdAndUsername() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "testuser");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getEventPayload()).contains("123e4567-e89b-12d3-a456-426655440001");
    }

    @Test
    void recover_whenRepositorySaveThrows_exceptionPropagates() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException originalException = new OptimisticLockingFailureException("original");
        doThrow(new RuntimeException("save failed")).when(failedEventRepository).save(any());

        // when / then
        assertThatThrownBy(() -> sut.recover(originalException, event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("save failed");
    }

    @Test
    void handlerMethod_isPublic() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void recoverMethod_isPublic() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_returnsVoid() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void recoverMethod_returnsVoid() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handlerMethod_retryableDoesNotExcludeAnyExceptions() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        // then
        assertThat(retryable.noRetryFor()).isEmpty();
    }

    @Test
    void handleStudentEnrolledToCourseEvent_cannotAcquireLockException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new CannotAcquireLockException("lock timeout"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(CannotAcquireLockException.class)
                .hasMessage("lock timeout");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_dataIntegrityViolationException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("constraint violation");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_dataIntegrityViolationException_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try {
            sut.handleStudentEnrolledToCourseEvent(event);
        } catch (DataIntegrityViolationException ignored) {
        }

        // then
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void recover_setsNonNullTimestampOnFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getTimestamp()).isNotNull();
    }

    @Test
    void recover_withCannotAcquireLockException_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final CannotAcquireLockException exception = new CannotAcquireLockException("lock timeout");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("lock timeout");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void class_hasNoRecoverMethodAcceptingGenericException() {
        // @Recover must accept TransientDataAccessException (not Exception) to prevent
        // business exceptions from triggering dead-letter persistence
        assertThatThrownBy(() ->
                StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                        "recover", Exception.class, StudentEnrolledToCourseIntegrationEvent.class)
        ).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void class_hasNoRecoverMethodAcceptingDataAccessException() {
        // @Recover must accept TransientDataAccessException (not DataAccessException) to
        // prevent non-transient exceptions like DataIntegrityViolationException from being
        // silently swallowed with inaccurate retry metadata
        assertThatThrownBy(() ->
                StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                        "recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class)
        ).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void recoverMethod_parameterTypeCoversAllRetryableExceptions() throws NoSuchMethodException {
        Method handlerMethod = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Retryable retryable = handlerMethod.getAnnotation(Retryable.class);

        Method recoverMethod = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);
        Class<?> recoverExceptionType = recoverMethod.getParameterTypes()[0];

        for (Class<? extends Throwable> retryForType : retryable.retryFor()) {
            assertThat(recoverExceptionType.isAssignableFrom(retryForType))
                    .as("@Recover param %s should be assignable from retryFor type %s",
                            recoverExceptionType.getSimpleName(), retryForType.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void handleStudentEnrolledToCourseEvent_exceptionWithCause_preservesCauseChain() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final RuntimeException rootCause = new RuntimeException("root cause");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("lock failed", rootCause);
        doThrow(exception).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasCause(rootCause);
    }

    @Test
    void recover_retryCountMatchesRetryableMaxAttempts() throws NoSuchMethodException {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        int maxAttempts = method.getAnnotation(Retryable.class).maxAttempts();

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getRetryCount()).isEqualTo(maxAttempts);
    }

    @Test
    void recover_withNullMessagePessimisticLocking_usesClassName() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final PessimisticLockingFailureException exception = new PessimisticLockingFailureException(null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage()).isEqualTo(PessimisticLockingFailureException.class.getName());
    }

    @Test
    void constructor_requiresBothDependencies() throws NoSuchMethodException {
        // when
        var constructor = StudentEnrolledToCourseIntegrationEventHandler.class.getConstructor(
                IncreaseNumberOfStudentsCommandHandler.class,
                com.educational.platform.common.event.FailedIntegrationEventRepository.class);

        // then
        assertThat(constructor).isNotNull();
        assertThat(constructor.getParameterCount()).isEqualTo(2);
    }

    @Test
    void handlerMethod_hasExactlyOneParameter() throws NoSuchMethodException {
        // @EventListener requires exactly one parameter (the event type)
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class);
    }

    @Test
    void recoverMethod_hasExactlyTwoParameters() throws NoSuchMethodException {
        // @Recover requires (exception, event) parameter signature
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.getParameterCount()).isEqualTo(2);
        assertThat(method.getParameterTypes()[0]).isEqualTo(TransientDataAccessException.class);
        assertThat(method.getParameterTypes()[1]).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class);
    }

    @Test
    void handlerAndRecoverMethods_haveMatchingReturnTypes() throws NoSuchMethodException {
        // Spring Retry requires @Recover return type to match the retryable method
        // when
        Method handlerMethod = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Method recoverMethod = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(handlerMethod.getReturnType()).isEqualTo(recoverMethod.getReturnType());
    }

    @Test
    void retryableAnnotation_matchesIntegrationEventRetryHandlerExceptions() throws NoSuchMethodException {
        // Verify handler's @Retryable config is consistent with centralized RETRYABLE_EXCEPTIONS
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        // then
        assertThat(retryable.retryFor()).containsExactlyInAnyOrder(
                (Class[]) com.educational.platform.common.event.IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS
        );
    }

    @Test
    void class_hasNoRecoverMethodAcceptingRuntimeException() {
        assertThatThrownBy(() ->
                StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                        "recover", RuntimeException.class, StudentEnrolledToCourseIntegrationEvent.class)
        ).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void recoverMethod_doesNotHaveEventListenerOrAsyncAnnotation() throws NoSuchMethodException {
        // when
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.isAnnotationPresent(EventListener.class)).isFalse();
        assertThat(method.isAnnotationPresent(Async.class)).isFalse();
    }

    @Test
    void recover_withConcurrencyFailureException_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final ConcurrencyFailureException exception = new ConcurrencyFailureException("concurrency failure");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("concurrency failure");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_nullPointerException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new NullPointerException("null reference"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("null reference");
    }

    @Test
    void recover_withExceptionHavingMessageAndCause_persistsOnlyTopLevelMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final RuntimeException cause = new RuntimeException("root cause message");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("top level message", cause);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage())
                .isEqualTo("top level message")
                .doesNotContain("root cause message");
    }

    @Test
    void recover_timestampIsRecentlyGenerated() {
        // given
        final Instant before = Instant.now();
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final Instant after = Instant.now();
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getTimestamp())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    void recover_eventPayloadContainsBothCourseIdAndUsername() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "john.doe");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getEventPayload())
                .contains("123e4567-e89b-12d3-a456-426655440001")
                .contains("john.doe");
    }

    @Test
    void recover_eventClassNameMatchesActualEventClass() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getEventClassName())
                .isEqualTo("com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_calledTwiceWithDifferentEvents_commandHandlerCalledTwice() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(uuid1, "user1");
        final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(uuid2, "user2");

        // when
        sut.handleStudentEnrolledToCourseEvent(event1);
        sut.handleStudentEnrolledToCourseEvent(event2);

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(2)).handle(any());
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void recover_calledTwiceWithDifferentEvents_savesTwoSeparateRecords() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(uuid1, "user1");
        final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(uuid2, "user2");
        final OptimisticLockingFailureException exception1 = new OptimisticLockingFailureException("error 1");
        final PessimisticLockingFailureException exception2 = new PessimisticLockingFailureException("error 2");

        // when
        sut.recover(exception1, event1);
        sut.recover(exception2, event2);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository, times(2)).save(argument.capture());
        assertThat(argument.getAllValues()).hasSize(2);
        assertThat(argument.getAllValues().get(0).getExceptionMessage()).isEqualTo("error 1");
        assertThat(argument.getAllValues().get(1).getExceptionMessage()).isEqualTo("error 2");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_deadlockLoserDataAccessException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DeadlockLoserDataAccessException("deadlock victim", null))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(DeadlockLoserDataAccessException.class)
                .hasMessage("deadlock victim");
    }

    @Test
    void recover_withDeadlockLoserDataAccessException_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final DeadlockLoserDataAccessException exception = new DeadlockLoserDataAccessException("deadlock victim", null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("deadlock victim");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_transientDataAccessResourceException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new TransientDataAccessResourceException("connection pool exhausted"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(TransientDataAccessResourceException.class)
                .hasMessage("connection pool exhausted");
    }

    @Test
    void recover_withTransientDataAccessResourceException_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final TransientDataAccessResourceException exception = new TransientDataAccessResourceException("connection pool exhausted");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("connection pool exhausted");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void recover_eventPayloadContainsUsername() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "testuser");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getEventPayload()).contains("testuser");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_successFollowedByException_bothEventsProcessed() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(uuid1, "user1");
        final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(uuid2, "user2");

        org.mockito.Mockito.doNothing()
                .doThrow(new OptimisticLockingFailureException("lock on second"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when - first event succeeds
        sut.handleStudentEnrolledToCourseEvent(event1);

        // second event fails
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event2))
                .isInstanceOf(OptimisticLockingFailureException.class);

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(2)).handle(any());
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_errorSubclass_propagatesUnchanged() {
        // given - StackOverflowError is an Error, not an Exception, so it bypasses catch(Exception)
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final StackOverflowError error = new StackOverflowError("stack overflow in handler");
        org.mockito.Mockito.doThrow(error).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(StackOverflowError.class)
                .hasMessage("stack overflow in handler");
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_passesCorrectCourseIdToCommand() {
        // given
        final UUID uuid = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> argument = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(argument.capture());
        assertThat(argument.getValue()).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void recover_withMultipleFieldsPopulated_allFieldsArePersisted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "testuser");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("all fields test");

        // when
        final Instant before = Instant.now();
        sut.recover(exception, event);
        final Instant after = Instant.now();

        // then - all 7 fields of FailedIntegrationEventRecord are verified
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord record = argument.getValue();
        assertThat(record.getId()).isNull();
        assertThat(record.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(record.getEventPayload()).isEqualTo(event.toString());
        assertThat(record.getExceptionMessage()).isEqualTo("all fields test");
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getTimestamp()).isBetween(before, after);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void recover_retryCountIsAlwaysThree_matchingMaxAttempts() {
        // given - the retry count persisted must match @Retryable(maxAttempts=3)
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("lock");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getRetryCount())
                .as("Persisted retry count must equal @Retryable.maxAttempts")
                .isEqualTo(3);
    }

    @Test
    void recover_exceptionWithCauseChain_persistsOnlyTopLevelMessage() {
        // given - exception with nested cause; only the top-level message should be persisted
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final RuntimeException rootCause = new RuntimeException("root cause detail");
        final QueryTimeoutException exception = new QueryTimeoutException("top level timeout", rootCause);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage())
                .isEqualTo("top level timeout")
                .doesNotContain("root cause detail");
    }

}
