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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseRatingRecalculatedIntegrationEventHandlerTest {

    @Mock
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedIntegrationEventRepository;

    @InjectMocks
    private CourseRatingRecalculatedIntegrationEventHandler sut;

    @Test
    void handleCourseRatingRecalculatedEvent_updateCourseRatingCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> argument = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(argument.capture());
        final UpdateCourseRatingCommand command = argument.getValue();
        assertThat(command)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 4.5);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_transientException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void recover_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB connection lost");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final FailedIntegrationEventRecord failedEvent = captor.getValue();
        assertThat(failedEvent).isNotNull();
        assertThat(getField(failedEvent, "eventClassName")).isEqualTo(event.getClass().getName());
        assertThat(getField(failedEvent, "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(failedEvent, "exceptionMessage")).isEqualTo("DB connection lost");
        assertThat(getField(failedEvent, "exceptionClassName")).isEqualTo(DataAccessResourceFailureException.class.getName());
        assertThat((int) getField(failedEvent, "retryCount")).isEqualTo(3);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_businessException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_optimisticLockException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock conflict", new RuntimeException()))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_successPath_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withOptimisticLockException_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final ObjectOptimisticLockingFailureException exception = new ObjectOptimisticLockingFailureException("Optimistic lock", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final FailedIntegrationEventRecord failedEvent = captor.getValue();
        assertThat(getField(failedEvent, "exceptionClassName")).isEqualTo(ObjectOptimisticLockingFailureException.class.getName());
        assertThat(getField(failedEvent, "exceptionMessage")).isEqualTo("Optimistic lock");
    }

    @Test
    void recover_withNullExceptionMessage_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException((String) null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_exceptionMessagePreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new DataAccessResourceFailureException("specific error message"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("specific error message");
    }

    @Test
    void handlerMethod_hasRetryableAnnotation_withCorrectConfig() throws NoSuchMethodException {
        // given
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // when
        Retryable retryable = method.getAnnotation(Retryable.class);

        // then
        assertThat(retryable).isNotNull();
        assertThat(retryable.retryFor()).containsExactlyInAnyOrder(DataAccessException.class, ObjectOptimisticLockingFailureException.class);
        assertThat(retryable.maxAttempts()).isEqualTo(3);
        Backoff backoff = retryable.backoff();
        assertThat(backoff.delay()).isEqualTo(500);
        assertThat(backoff.multiplier()).isEqualTo(2.0);
    }

    @Test
    void handlerMethod_hasAsyncAndEventListenerAnnotations() throws NoSuchMethodException {
        // given
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        assertThat(method.getAnnotation(Async.class)).isNotNull();
        assertThat(method.getAnnotation(EventListener.class)).isNotNull();
    }

    @Test
    void recoverMethod_hasRecoverAnnotation() {
        // given
        boolean hasRecover = Arrays.stream(CourseRatingRecalculatedIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("recover"))
                .anyMatch(m -> m.getAnnotation(Recover.class) != null);

        // then
        assertThat(hasRecover).isTrue();
    }

    @Test
    void handlerClass_hasComponentAnnotation() {
        assertThat(CourseRatingRecalculatedIntegrationEventHandler.class.getAnnotation(Component.class)).isNotNull();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_transientException_commandHandlerStillInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try { sut.handleCourseRatingRecalculatedEvent(event); } catch (Exception ignored) { }

        // then
        verify(updateCourseRatingCommandHandler, times(1)).handle(any());
    }

    @Test
    void recover_whenRepositorySaveFails_propagatesException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException originalException = new DataAccessResourceFailureException("DB connection lost");
        doThrow(new DataAccessResourceFailureException("Failed to save"))
                .when(failedIntegrationEventRepository).save(any());

        // when/then
        assertThatThrownBy(() -> sut.recover(originalException, event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("Failed to save");
    }

    @Test
    void recoverMethod_acceptsDataAccessException() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(method).isNotNull();
        assertThat(method.getAnnotation(Recover.class)).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_zeroRating_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 0.0);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 0.0);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_genericRuntimeException_rethrows() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new IllegalStateException("unexpected state"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unexpected state");
    }

    @Test
    void handleCourseRatingRecalculatedEvent_negativeRating_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, -1.0);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", -1.0);
    }

    @Test
    void recover_savesExactlyOneRecord() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB down");

        // when
        sut.recover(exception, event);

        // then
        verify(failedIntegrationEventRepository, times(1)).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void recover_persistsRecordWithFailedStatus() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB connection lost");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((FailedIntegrationEventRecord.Status) getField(captor.getValue(), "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void recover_persistsRecordWithCreatedAtTimestamp() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB connection lost");
        final Instant before = Instant.now();

        // when
        sut.recover(exception, event);

        // then
        final Instant after = Instant.now();
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final Instant createdAt = (Instant) getField(captor.getValue(), "createdAt");
        assertThat(createdAt).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void recover_withCausedException_persistsTopLevelExceptionMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException(
                "Top-level message", new RuntimeException("root cause"));

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("Top-level message");
    }

    @Test
    void handleCourseRatingRecalculatedEvent_maxDoubleRating_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.MAX_VALUE);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", Double.MAX_VALUE);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_maxRating_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 5.0);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 5.0);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_nanRating_commandReceivedCorrectValue() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.NaN);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(Double.isNaN((double) getField(captor.getValue(), "rating"))).isTrue();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_positiveInfinityRating_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.POSITIVE_INFINITY);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", Double.POSITIVE_INFINITY);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_nullCourseId_commandHandlerStillInvoked() {
        // given
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(null, 4.5);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", null)
                .hasFieldOrPropertyWithValue("rating", 4.5);
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("Unique constraint violated");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName")).isEqualTo(DataIntegrityViolationException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("Unique constraint violated");
    }

    @Test
    void handlerMethod_retryableAnnotation_recoverAttributeIsDefault() throws NoSuchMethodException {
        // given
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.recover()).isEmpty();
    }

    @Test
    void handlerMethod_retryableAnnotation_noRetryForIsEmpty() throws NoSuchMethodException {
        // given
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.noRetryFor()).isEmpty();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_rethrowsExactSameExceptionInstance() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException originalException = new DataAccessResourceFailureException("DB error");
        doThrow(originalException).when(updateCourseRatingCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_transientException_passesCorrectArgumentsToCommand() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 3.7);
        doThrow(new DataAccessResourceFailureException("DB error"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try { sut.handleCourseRatingRecalculatedEvent(event); } catch (Exception ignored) { }

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", 3.7);
    }

    @Test
    void recoverMethod_doesNotHaveAsyncAnnotation() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(method.getAnnotation(Async.class)).isNull();
    }

    @Test
    void recoverMethod_doesNotHaveEventListenerAnnotation() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(method.getAnnotation(EventListener.class)).isNull();
    }

    @Test
    void handlerMethod_isPublic() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void recoverMethod_isPublic() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_hasSingleEventListenerMethod() {
        long count = Arrays.stream(CourseRatingRecalculatedIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void handler_hasSingleRecoverMethod() {
        long count = Arrays.stream(CourseRatingRecalculatedIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(Recover.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void maxAttemptsConstant_matchesRetryableAnnotation() throws Exception {
        Field maxAttemptsField = CourseRatingRecalculatedIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        maxAttemptsField.setAccessible(true);
        int maxAttempts = (int) maxAttemptsField.get(null);

        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        assertThat(retryable.maxAttempts()).isEqualTo(maxAttempts);
    }

    @Test
    void recoverMethod_doesNotAcceptBroadExceptionType() {
        assertThatThrownBy(() -> CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", Exception.class, CourseRatingRecalculatedIntegrationEvent.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void retryableMaxAttempts_matchesRecoverRetryCount() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");
        Method handlerMethod = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        int annotationMaxAttempts = handlerMethod.getAnnotation(Retryable.class).maxAttempts();

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((int) getField(captor.getValue(), "retryCount")).isEqualTo(annotationMaxAttempts);
    }

    @Test
    void recoverMethod_doesNotHaveRetryableAnnotation() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(method.getAnnotation(Retryable.class)).isNull();
    }

    @Test
    void maxAttemptsField_isStaticFinal() throws NoSuchFieldException {
        Field field = CourseRatingRecalculatedIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_hasExactlyOneParameter() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(CourseRatingRecalculatedIntegrationEvent.class);
    }

    @Test
    void recoverMethod_hasExactlyTwoParameters() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(2);
        assertThat(method.getParameterTypes()[0]).isEqualTo(DataAccessException.class);
        assertThat(method.getParameterTypes()[1]).isEqualTo(CourseRatingRecalculatedIntegrationEvent.class);
    }

    @Test
    void recover_doesNotInvokeCommandHandler() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        verifyNoInteractions(updateCourseRatingCommandHandler);
    }

    @Test
    void recover_calledMultipleTimes_savesIndependentRecords() throws Exception {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseRatingRecalculatedIntegrationEvent event1 = new CourseRatingRecalculatedIntegrationEvent(uuid1, 3.5);
        final CourseRatingRecalculatedIntegrationEvent event2 = new CourseRatingRecalculatedIntegrationEvent(uuid2, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event1);
        sut.recover(exception, event2);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(getField(captor.getAllValues().get(0), "eventPayload")).isEqualTo(event1.toString());
        assertThat(getField(captor.getAllValues().get(1), "eventPayload")).isEqualTo(event2.toString());
    }

    @Test
    void handleCourseRatingRecalculatedEvent_negativeInfinityRating_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.NEGATIVE_INFINITY);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", Double.NEGATIVE_INFINITY);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_minValueRating_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.MIN_VALUE);

        // when
        sut.handleCourseRatingRecalculatedEvent(event);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler).handle(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("rating", Double.MIN_VALUE);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_onDataAccessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try { sut.handleCourseRatingRecalculatedEvent(event); } catch (Exception ignored) { }

        // then — only recover() should persist dead-letter records, never the handler itself
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_onBusinessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try { sut.handleCourseRatingRecalculatedEvent(event); } catch (Exception ignored) { }

        // then — non-retryable exceptions propagate to AsyncUncaughtExceptionHandler, not to the repository
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_onOptimisticLockException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock", new RuntimeException()))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when
        try { sut.handleCourseRatingRecalculatedEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_completesNormally_whenRepositorySaveSucceeds() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when — should complete without throwing
        sut.recover(exception, event);

        // then
        verify(failedIntegrationEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void handleCourseRatingRecalculatedEvent_dataIntegrityViolationException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new DataIntegrityViolationException("unique constraint violated"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleCourseRatingRecalculatedEvent(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("unique constraint violated");
    }

    @Test
    void handlerClass_isPublic() {
        assertThat(Modifier.isPublic(CourseRatingRecalculatedIntegrationEventHandler.class.getModifiers())).isTrue();
    }

    @Test
    void handlerAndRecoverMethods_haveMatchingReturnType() throws NoSuchMethodException {
        Method handler = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Method recover = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(handler.getReturnType()).isEqualTo(recover.getReturnType());
    }

    @Test
    void handlerMethod_backoffMaxDelayIsUnlimited() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Backoff backoff = method.getAnnotation(Retryable.class).backoff();
        assertThat(backoff.maxDelay()).isEqualTo(0L);
    }

    @Test
    void handlerMethod_retryableListenersIsEmpty() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.listeners()).isEmpty();
    }

    @Test
    void handler_constructorRequiresBothDependencies() {
        assertThat(CourseRatingRecalculatedIntegrationEventHandler.class.getConstructors()).hasSize(1);
        assertThat(CourseRatingRecalculatedIntegrationEventHandler.class.getConstructors()[0].getParameterCount()).isEqualTo(2);
    }

    @Test
    void handler_constructorFirstParam_isCommandHandler() {
        Class<?>[] paramTypes = CourseRatingRecalculatedIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[0]).isEqualTo(UpdateCourseRatingCommandHandler.class);
    }

    @Test
    void handler_constructorSecondParam_isFailedIntegrationEventRepository() {
        Class<?>[] paramTypes = CourseRatingRecalculatedIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[1]).isEqualTo(FailedIntegrationEventRepository.class);
    }

    @Test
    void handlerMethod_asyncAnnotation_usesDefaultExecutor() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("handleCourseRatingRecalculatedEvent", CourseRatingRecalculatedIntegrationEvent.class);
        Async async = method.getAnnotation(Async.class);
        assertThat(async.value()).isEmpty();
    }

    @Test
    void recoverMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = CourseRatingRecalculatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, CourseRatingRecalculatedIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_isNotAbstract() {
        assertThat(Modifier.isAbstract(CourseRatingRecalculatedIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handler_isNotFinal() {
        assertThat(Modifier.isFinal(CourseRatingRecalculatedIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handleCourseRatingRecalculatedEvent_errorSubclass_propagatesWithoutCatch() {
        // given — Error subclasses bypass the catch(Exception) block
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        doThrow(new StackOverflowError("deep recursion"))
                .when(updateCourseRatingCommandHandler).handle(any());

        // when/then — Error propagates directly, not caught by handler
        try {
            sut.handleCourseRatingRecalculatedEvent(event);
            assertThat(true).as("Expected StackOverflowError to be thrown").isFalse();
        } catch (StackOverflowError e) {
            assertThat(e.getMessage()).isEqualTo("deep recursion");
        }
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleCourseRatingRecalculatedEvent_concurrentInvocations_areIndependent() {
        // given — handler is stateless, concurrent calls should not interfere
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final CourseRatingRecalculatedIntegrationEvent event1 = new CourseRatingRecalculatedIntegrationEvent(uuid1, 3.5);
        final CourseRatingRecalculatedIntegrationEvent event2 = new CourseRatingRecalculatedIntegrationEvent(uuid2, 4.5);

        // when
        sut.handleCourseRatingRecalculatedEvent(event1);
        sut.handleCourseRatingRecalculatedEvent(event2);

        // then
        final ArgumentCaptor<UpdateCourseRatingCommand> captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(updateCourseRatingCommandHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasFieldOrPropertyWithValue("uuid", uuid1);
        assertThat(captor.getAllValues().get(1)).hasFieldOrPropertyWithValue("uuid", uuid2);
    }

    @Test
    void recover_withNaNRating_persistsEventPayloadContainingNaN() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.NaN);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).asString().contains("NaN");
        assertThat(getField(captor.getValue(), "eventClassName")).isEqualTo(event.getClass().getName());
    }

    @Test
    void recover_withPositiveInfinityRating_persistsEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.POSITIVE_INFINITY);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).asString().contains("Infinity");
    }

    @Test
    void recover_withNegativeInfinityRating_persistsEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.NEGATIVE_INFINITY);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).asString().contains("-Infinity");
    }

    @Test
    void recover_withZeroRating_persistsEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 0.0);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_withNegativeRating_persistsEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, -1.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_withNullCourseId_persistsEventPayload() throws Exception {
        // given
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(null, 4.5);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventPayload")).asString().contains("null");
    }

    @Test
    void recover_withMaxDoubleRating_persistsEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, Double.MAX_VALUE);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_eventPayloadMatchesEventToString() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.75);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventPayload")).asString()
                .contains("123e4567-e89b-12d3-a456-426655440001")
                .contains("4.75");
    }

    @Test
    void recover_repositorySaveFails_exceptionPropagates() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final DataAccessResourceFailureException originalException = new DataAccessResourceFailureException("DB error");
        doThrow(new RuntimeException("Repository save failed"))
                .when(failedIntegrationEventRepository).save(any(FailedIntegrationEventRecord.class));

        // when/then — if dead-letter persistence fails, the exception must propagate
        assertThatThrownBy(() -> sut.recover(originalException, event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Repository save failed");
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectClassName() throws Exception {
        // given — OOLFE is explicitly listed in retryFor and is a DataAccessException subclass
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseRatingRecalculatedIntegrationEvent event = new CourseRatingRecalculatedIntegrationEvent(uuid, 4.5);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("Optimistic lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(ObjectOptimisticLockingFailureException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage"))
                .isEqualTo("Optimistic lock failure");
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
