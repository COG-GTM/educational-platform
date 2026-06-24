package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class CourseRatingRecalculatedIntegrationEventHandlerTest {

    @Mock
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedEventRepository;

    @InjectMocks
    private CourseRatingRecalculatedIntegrationEventHandler sut;

    @Test
    void handleCourseRatingRecalculatedEvent_updateCourseRatingCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> argument = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(argument.capture());
        final UpdateCourseRatingCommand updateCourseRatingCommand = argument.getValue();
        assertThat(updateCourseRatingCommand)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 3.7);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_transientException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_businessException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_pessimisticLockingException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new PessimisticLockingFailureException("pessimistic lock"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(PessimisticLockingFailureException.class);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_successfulHandling_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("DB connection lost");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(CourseRatingRecalculatedIntegrationEvent.class.getName());
        assertThat(failedEvent.getEventPayload()).isEqualTo(event.toString());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("DB connection lost");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void recover_withNullExceptionMessage_usesExceptionClassName() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
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
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
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
    void handleCourseRatingRecalculatedEvent_genericRuntimeException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new IllegalStateException("invalid state"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("invalid state");
    }

    @Test
    void handleCourseRatingRecalculatedEvent_transientException_preservesOriginalMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new OptimisticLockingFailureException("specific DB error"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("specific DB error");
    }

    @Test
    void handleCourseRatingRecalculatedEvent_transientException_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try {
            sut.handleCourseRatingRecalculatedEvent(event);
        } catch (OptimisticLockingFailureException ignored) {
        }

        // then
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void handlerMethod_hasRetryableAnnotationWithCorrectConfig() throws NoSuchMethodException {
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
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
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(method.isAnnotationPresent(Async.class)).isTrue();
        assertThat(method.isAnnotationPresent(EventListener.class)).isTrue();
    }

    @Test
    void recoverMethod_hasRecoverAnnotationWithCorrectParameterTypes() throws NoSuchMethodException {
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(method.isAnnotationPresent(Recover.class)).isTrue();
    }

    @Test
    void class_hasComponentAnnotation() {
        // then
        assertThat(CourseRatingRecalculatedIntegrationEventHandler.class.isAnnotationPresent(Component.class)).isTrue();
    }

    @Test
    void recover_withEmptyExceptionMessage_persistsEmptyMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
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
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        final QueryTimeoutException exception = new QueryTimeoutException("query timed out");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(CourseRatingRecalculatedIntegrationEvent.class.getName());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("query timed out");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_businessException_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try {
            sut.handleCourseRatingRecalculatedEvent(event);
        } catch (ResourceNotFoundException ignored) {
        }

        // then
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_queryTimeoutException_rethrown() {
        // given - QueryTimeoutException is a TransientDataAccessException subclass
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new QueryTimeoutException("query timed out"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(QueryTimeoutException.class)
                .hasMessage("query timed out");
    }

    @Test
    void recover_withQueryTimeoutException_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final QueryTimeoutException exception = new QueryTimeoutException("query timed out");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(CourseRatingRecalculatedIntegrationEvent.class.getName());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("query timed out");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_passesCorrectRatingToCommand() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.95);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> argument = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 4.95);
    }

    @Test
    void recover_eventPayloadContainsCourseIdAndRating() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
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
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
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
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void recoverMethod_isPublic() throws NoSuchMethodException {
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_returnsVoid() throws NoSuchMethodException {
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void recoverMethod_returnsVoid() throws NoSuchMethodException {
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handlerMethod_retryableDoesNotExcludeAnyExceptions() throws NoSuchMethodException {
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        // then
        assertThat(retryable.noRetryFor()).isEmpty();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_cannotAcquireLockException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new CannotAcquireLockException("lock timeout"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(CannotAcquireLockException.class)
                .hasMessage("lock timeout");
    }

    @Test
    void handleCourseRatingRecalculatedEvent_dataIntegrityViolationException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("constraint violation");
    }

    @Test
    void handleCourseRatingRecalculatedEvent_dataIntegrityViolationException_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try {
            sut.handleCourseRatingRecalculatedEvent(event);
        } catch (DataIntegrityViolationException ignored) {
        }

        // then
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void recover_setsNonNullTimestampOnFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
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
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
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
    void recover_eventPayloadContainsRatingValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.95);
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getEventPayload()).contains("4.95");
    }

    @Test
    void class_hasNoRecoverMethodAcceptingGenericException() {
        // @Recover must accept TransientDataAccessException (not Exception) to prevent
        // business exceptions from triggering dead-letter persistence
        assertThatThrownBy(() ->
                CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                        "recover", Exception.class, CourseRatingRecalculatedIntegrationEvent.class)
        ).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void class_hasNoRecoverMethodAcceptingDataAccessException() {
        // @Recover must accept TransientDataAccessException (not DataAccessException) to
        // prevent non-transient exceptions like DataIntegrityViolationException from being
        // silently swallowed with inaccurate retry metadata
        assertThatThrownBy(() ->
                CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                        "recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class)
        ).isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void recoverMethod_parameterTypeCoversAllRetryableExceptions() throws NoSuchMethodException {
        Method handlerMethod = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Retryable retryable = handlerMethod.getAnnotation(Retryable.class);

        Method recoverMethod = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);
        Class<?> recoverExceptionType = recoverMethod.getParameterTypes()[0];

        for (Class<? extends Throwable> retryForType : retryable.retryFor()) {
            assertThat(recoverExceptionType.isAssignableFrom(retryForType))
                    .as("@Recover param %s should be assignable from retryFor type %s",
                            recoverExceptionType.getSimpleName(), retryForType.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void handleCourseRatingRecalculatedEvent_exceptionWithCause_preservesCauseChain() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        final RuntimeException rootCause = new RuntimeException("root cause");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("lock failed", rootCause);
        doThrow(exception).when(updateCourseRatingCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasCause(rootCause);
    }

    @Test
    void recover_retryCountMatchesRetryableMaxAttempts() throws NoSuchMethodException {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("error");

        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
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
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
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
        var constructor = CourseRatingRecalculatedIntegrationEventHandler.class.getConstructor(
                UpdateCourseRatingCommandHandler.class,
                com.educational.platform.common.event.FailedIntegrationEventRepository.class);

        // then
        assertThat(constructor).isNotNull();
        assertThat(constructor.getParameterCount()).isEqualTo(2);
    }

    @Test
    void handlerMethod_hasExactlyOneParameter() throws NoSuchMethodException {
        // @EventListener requires exactly one parameter (the event type)
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(CourseRatingRecalculatedIntegrationEvent.class);
    }

    @Test
    void recoverMethod_hasExactlyTwoParameters() throws NoSuchMethodException {
        // @Recover requires (exception, event) parameter signature
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(method.getParameterCount()).isEqualTo(2);
        assertThat(method.getParameterTypes()[0]).isEqualTo(TransientDataAccessException.class);
        assertThat(method.getParameterTypes()[1]).isEqualTo(CourseRatingRecalculatedIntegrationEvent.class);
    }

    @Test
    void handlerAndRecoverMethods_haveMatchingReturnTypes() throws NoSuchMethodException {
        // Spring Retry requires @Recover return type to match the retryable method
        // when
        Method handlerMethod = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Method recoverMethod = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "recover", TransientDataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(handlerMethod.getReturnType()).isEqualTo(recoverMethod.getReturnType());
    }

    @Test
    void retryableAnnotation_matchesIntegrationEventRetryHandlerExceptions() throws NoSuchMethodException {
        // Verify handler's @Retryable config is consistent with centralized RETRYABLE_EXCEPTIONS
        // when
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class.getMethod(
                "handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        // then
        assertThat(retryable.retryFor()).containsExactlyInAnyOrder(
                (Class[]) com.educational.platform.common.event.IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS
        );
    }

    @Test
    void handleCourseRatingRecalculatedEvent_withZeroRating_passesCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 0.0);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> argument = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", 0.0);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_withMaxRating_passesCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 5.0);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> argument = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("rating", 5.0);
    }

}
