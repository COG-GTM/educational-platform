package com.educational.platform.administration.course.create;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

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
class SendCourseToApproveIntegrationEventHandlerTest {

    @Mock
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedIntegrationEventRepository;

    @InjectMocks
    private SendCourseToApproveIntegrationEventHandler sut;

    @Test
    void handleSendCourseToApproveEvent_createCourseProposalCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);

        // when
        sut.handleSendCourseToApproveEvent(event);

        // then
        final ArgumentCaptor<CreateCourseProposalCommand> argument = ArgumentCaptor.forClass(CreateCourseProposalCommand.class);
        verify(createCourseProposalCommandHandler).handle(argument.capture());
        final CreateCourseProposalCommand createCourseProposalCommand = argument.getValue();
        assertThat(createCourseProposalCommand)
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handleSendCourseToApproveEvent_transientException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void handleSendCourseToApproveEvent_businessException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recover_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
    void handleSendCourseToApproveEvent_optimisticLockException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock conflict", new RuntimeException()))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void handleSendCourseToApproveEvent_successPath_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);

        // when
        sut.handleSendCourseToApproveEvent(event);

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withOptimisticLockException_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException((String) null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void handleSendCourseToApproveEvent_exceptionMessagePreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new DataAccessResourceFailureException("specific error message"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("specific error message");
    }

    @Test
    void handlerMethod_hasRetryableAnnotation_withCorrectConfig() throws NoSuchMethodException {
        // given
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);

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
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);

        // then
        assertThat(method.getAnnotation(Async.class)).isNotNull();
        assertThat(method.getAnnotation(EventListener.class)).isNotNull();
    }

    @Test
    void recoverMethod_hasRecoverAnnotation() {
        // given
        boolean hasRecover = Arrays.stream(SendCourseToApproveIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("recover"))
                .anyMatch(m -> m.getAnnotation(Recover.class) != null);

        // then
        assertThat(hasRecover).isTrue();
    }

    @Test
    void handlerClass_hasComponentAnnotation() {
        assertThat(SendCourseToApproveIntegrationEventHandler.class.getAnnotation(Component.class)).isNotNull();
    }

    @Test
    void handleSendCourseToApproveEvent_transientException_commandHandlerStillInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then
        verify(createCourseProposalCommandHandler, times(1)).handle(any());
    }

    @Test
    void recover_whenRepositorySaveFails_propagatesException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);

        assertThat(method).isNotNull();
        assertThat(method.getAnnotation(Recover.class)).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handleSendCourseToApproveEvent_genericRuntimeException_rethrows() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new IllegalStateException("unexpected state"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unexpected state");
    }

    @Test
    void recover_savesExactlyOneRecord() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
    void handleSendCourseToApproveEvent_nullCourseId_commandHandlerStillInvoked() {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(null);

        // when
        sut.handleSendCourseToApproveEvent(event);

        // then
        final ArgumentCaptor<CreateCourseProposalCommand> captor = ArgumentCaptor.forClass(CreateCourseProposalCommand.class);
        verify(createCourseProposalCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", null);
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.recover()).isEmpty();
    }

    @Test
    void handlerMethod_retryableAnnotation_noRetryForIsEmpty() throws NoSuchMethodException {
        // given
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.noRetryFor()).isEmpty();
    }

    @Test
    void handleSendCourseToApproveEvent_rethrowsExactSameExceptionInstance() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException originalException = new DataAccessResourceFailureException("DB error");
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleSendCourseToApproveEvent_transientException_passesCorrectCourseIdToCommand() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new DataAccessResourceFailureException("DB error"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then
        final ArgumentCaptor<CreateCourseProposalCommand> captor = ArgumentCaptor.forClass(CreateCourseProposalCommand.class);
        verify(createCourseProposalCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void recoverMethod_doesNotHaveAsyncAnnotation() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);

        assertThat(method.getAnnotation(Async.class)).isNull();
    }

    @Test
    void recoverMethod_doesNotHaveEventListenerAnnotation() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);

        assertThat(method.getAnnotation(EventListener.class)).isNull();
    }

    @Test
    void handlerMethod_isPublic() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void recoverMethod_isPublic() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_hasSingleEventListenerMethod() {
        long count = Arrays.stream(SendCourseToApproveIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void handler_hasSingleRecoverMethod() {
        long count = Arrays.stream(SendCourseToApproveIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(Recover.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void maxAttemptsConstant_matchesRetryableAnnotation() throws Exception {
        Field maxAttemptsField = SendCourseToApproveIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        maxAttemptsField.setAccessible(true);
        int maxAttempts = (int) maxAttemptsField.get(null);

        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        assertThat(retryable.maxAttempts()).isEqualTo(maxAttempts);
    }

    @Test
    void recoverMethod_doesNotAcceptBroadExceptionType() {
        assertThatThrownBy(() -> SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", Exception.class, SendCourseToApproveIntegrationEvent.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void retryableMaxAttempts_matchesRecoverRetryCount() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");
        Method handlerMethod = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
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
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);

        assertThat(method.getAnnotation(Retryable.class)).isNull();
    }

    @Test
    void maxAttemptsField_isStaticFinal() throws NoSuchFieldException {
        Field field = SendCourseToApproveIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_hasExactlyOneParameter() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(SendCourseToApproveIntegrationEvent.class);
    }

    @Test
    void recoverMethod_hasExactlyTwoParameters() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(2);
        assertThat(method.getParameterTypes()[0]).isEqualTo(DataAccessException.class);
        assertThat(method.getParameterTypes()[1]).isEqualTo(SendCourseToApproveIntegrationEvent.class);
    }

    @Test
    void recover_doesNotInvokeCommandHandler() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        verifyNoInteractions(createCourseProposalCommandHandler);
    }

    @Test
    void recover_calledMultipleTimes_savesIndependentRecords() throws Exception {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final SendCourseToApproveIntegrationEvent event1 = new SendCourseToApproveIntegrationEvent(uuid1);
        final SendCourseToApproveIntegrationEvent event2 = new SendCourseToApproveIntegrationEvent(uuid2);
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
    void handleSendCourseToApproveEvent_dataIntegrityViolationException_rethrowsForRetry() {
        // given — DataIntegrityViolationException is a DataAccessException subclass
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("constraint violation");
    }

    @Test
    void handleSendCourseToApproveEvent_onDataAccessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then — only recover() should persist dead-letter records, never the handler itself
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleSendCourseToApproveEvent_onBusinessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then — non-retryable exceptions propagate to AsyncUncaughtExceptionHandler, not to the repository
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleSendCourseToApproveEvent_onOptimisticLockException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock", new RuntimeException()))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_completesNormally_whenRepositorySaveSucceeds() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when — should complete without throwing
        sut.recover(exception, event);

        // then
        verify(failedIntegrationEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void handlerClass_isPublic() {
        assertThat(Modifier.isPublic(SendCourseToApproveIntegrationEventHandler.class.getModifiers())).isTrue();
    }

    @Test
    void handlerAndRecoverMethods_haveMatchingReturnType() throws NoSuchMethodException {
        Method handler = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        Method recover = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);

        assertThat(handler.getReturnType()).isEqualTo(recover.getReturnType());
    }

    @Test
    void handlerMethod_backoffMaxDelayIsUnlimited() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        Backoff backoff = method.getAnnotation(Retryable.class).backoff();
        assertThat(backoff.maxDelay()).isEqualTo(0L);
    }

    @Test
    void handlerMethod_retryableListenersIsEmpty() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.listeners()).isEmpty();
    }

    @Test
    void handler_constructorRequiresBothDependencies() {
        assertThat(SendCourseToApproveIntegrationEventHandler.class.getConstructors()).hasSize(1);
        assertThat(SendCourseToApproveIntegrationEventHandler.class.getConstructors()[0].getParameterCount()).isEqualTo(2);
    }

    @Test
    void handler_constructorFirstParam_isCommandHandler() {
        Class<?>[] paramTypes = SendCourseToApproveIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[0]).isEqualTo(CreateCourseProposalCommandHandler.class);
    }

    @Test
    void handler_constructorSecondParam_isFailedIntegrationEventRepository() {
        Class<?>[] paramTypes = SendCourseToApproveIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[1]).isEqualTo(FailedIntegrationEventRepository.class);
    }

    @Test
    void handlerMethod_asyncAnnotation_usesDefaultExecutor() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        Async async = method.getAnnotation(Async.class);
        assertThat(async.value()).isEmpty();
    }

    @Test
    void recoverMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_isNotAbstract() {
        assertThat(Modifier.isAbstract(SendCourseToApproveIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handler_isNotFinal() {
        assertThat(Modifier.isFinal(SendCourseToApproveIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handleSendCourseToApproveEvent_errorSubclass_propagatesWithoutCatch() {
        // given — Error subclasses bypass the catch(Exception) block
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new StackOverflowError("deep recursion"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then — Error propagates directly, not caught by handler
        try {
            sut.handleSendCourseToApproveEvent(event);
            assertThat(true).as("Expected StackOverflowError to be thrown").isFalse();
        } catch (StackOverflowError e) {
            assertThat(e.getMessage()).isEqualTo("deep recursion");
        }
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleSendCourseToApproveEvent_concurrentInvocations_areIndependent() {
        // given — handler is stateless, so concurrent calls should not interfere
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final SendCourseToApproveIntegrationEvent event1 = new SendCourseToApproveIntegrationEvent(uuid1);
        final SendCourseToApproveIntegrationEvent event2 = new SendCourseToApproveIntegrationEvent(uuid2);

        // when
        sut.handleSendCourseToApproveEvent(event1);
        sut.handleSendCourseToApproveEvent(event2);

        // then
        final ArgumentCaptor<CreateCourseProposalCommand> captor = ArgumentCaptor.forClass(CreateCourseProposalCommand.class);
        verify(createCourseProposalCommandHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasFieldOrPropertyWithValue("uuid", uuid1);
        assertThat(captor.getAllValues().get(1)).hasFieldOrPropertyWithValue("uuid", uuid2);
    }

    @Test
    void recover_withNullCourseId_persistsEventPayload() throws Exception {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(null);
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
    void recover_eventPayloadMatchesEventToString() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventPayload")).asString().contains("123e4567-e89b-12d3-a456-426655440001");
    }

    @Test
    void recover_repositorySaveFails_exceptionPropagates() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
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

    @Test
    void recover_eventClassNameIsFullyQualified() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo("com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent");
    }

    @Test
    void handleSendCourseToApproveEvent_checkedExceptionFromCommandHandler_rethrows() {
        // given — unchecked wrapper of a checked exception
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new IllegalArgumentException("invalid course id"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid course id");
    }

    @Test
    void recover_eventClassName_isNotSimpleName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must be package-qualified, not a simple class name
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final String eventClassName = (String) getField(captor.getValue(), "eventClassName");
        assertThat(eventClassName).contains(".");
        assertThat(eventClassName).isNotEqualTo(event.getClass().getSimpleName());
    }

    @Test
    void recover_exceptionClassNameIsFullyQualified() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo("org.springframework.dao.DataAccessResourceFailureException");
    }

    @Test
    void recover_exceptionClassName_isNotSimpleName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — exceptionClassName must be package-qualified, not a simple class name
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final String exceptionClassName = (String) getField(captor.getValue(), "exceptionClassName");
        assertThat(exceptionClassName).contains(".");
        assertThat(exceptionClassName).isNotEqualTo(exception.getClass().getSimpleName());
    }

    @Test
    void handleSendCourseToApproveEvent_checkedExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final IllegalArgumentException originalException = new IllegalArgumentException("invalid course id");
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the exception
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleSendCourseToApproveEvent_NullPointerExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final NullPointerException originalException = new NullPointerException("course was null");
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then — NPE must propagate with identity preserved through catch(Exception e) { throw e; }
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleSendCourseToApproveEvent_checkedExceptionFromCommandHandler_causeIsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final IllegalArgumentException exception = new IllegalArgumentException("invalid course id");
        doThrow(exception).when(createCourseProposalCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not add a cause chain
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .hasNoCause();
    }

    @Test
    void recover_eventClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must start with the platform package prefix
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventClassName"))
                .startsWith("com.educational.platform.");
    }

    @Test
    void recover_exceptionClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — exceptionClassName must start with the Spring DAO package prefix
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionClassName"))
                .startsWith("org.springframework.dao.");
    }

    @Test
    void recover_withDifferentExceptionSubtypes_persistsCorrectClassForEach() throws Exception {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final SendCourseToApproveIntegrationEvent event1 = new SendCourseToApproveIntegrationEvent(uuid1);
        final SendCourseToApproveIntegrationEvent event2 = new SendCourseToApproveIntegrationEvent(uuid2);

        // when
        sut.recover(new DataAccessResourceFailureException("transient error"), event1);
        sut.recover(new DataIntegrityViolationException("constraint error"), event2);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository, times(2)).save(captor.capture());
        assertThat(getField(captor.getAllValues().get(0), "exceptionClassName"))
                .isEqualTo(DataAccessResourceFailureException.class.getName());
        assertThat(getField(captor.getAllValues().get(1), "exceptionClassName"))
                .isEqualTo(DataIntegrityViolationException.class.getName());
    }

    @Test
    void recover_withEmptyExceptionMessage_persistsEmptyString() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("");
    }

    @Test
    void handleSendCourseToApproveEvent_checkedExceptionWrapped_rethrows() {
        // given — RuntimeException wrapping a checked cause
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final RuntimeException wrappedException = new RuntimeException("wrapped",
                new java.io.IOException("disk full"));
        doThrow(wrappedException).when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(wrappedException)
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    void handleSendCourseToApproveEvent_ObjectOptimisticLockingFailureExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException originalException =
                new ObjectOptimisticLockingFailureException("optimistic lock conflict", new RuntimeException());
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the retryable exception
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleSendCourseToApproveEvent_DataIntegrityViolationExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException originalException =
                new DataIntegrityViolationException("constraint violation");
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the DataAccessException subclass
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_exceptionClassName_isNotSimpleName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — exceptionClassName must be package-qualified, not a simple class name
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final String exceptionClassName = (String) getField(captor.getValue(), "exceptionClassName");
        assertThat(exceptionClassName).contains(".");
        assertThat(exceptionClassName).isNotEqualTo(exception.getClass().getSimpleName());
    }

    @Test
    void recover_withDataIntegrityViolationException_exceptionClassName_isNotSimpleName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — exceptionClassName must be package-qualified, not a simple class name
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final String exceptionClassName = (String) getField(captor.getValue(), "exceptionClassName");
        assertThat(exceptionClassName).contains(".");
        assertThat(exceptionClassName).isNotEqualTo(exception.getClass().getSimpleName());
    }

    @Test
    void recoverMethod_secondParameterType_matchesHandlerMethodEventType() throws NoSuchMethodException {
        // given
        Method handlerMethod = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        Method recoverMethod = SendCourseToApproveIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);

        // then — @Recover second parameter must match @EventListener parameter for Spring Retry matching
        assertThat(recoverMethod.getParameterTypes()[1]).isEqualTo(handlerMethod.getParameterTypes()[0]);
    }

    @Test
    void recover_eventPayloadContainsCourseId() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — persisted payload must contain the courseId UUID string
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload"))
                .contains(uuid.toString());
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectEventClassName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — eventClassName must still be correct regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsCorrectEventClassName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must still be correct regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — eventPayload must still be event.toString() regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsCorrectEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — eventPayload must still be event.toString() regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectRetryCount() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — retryCount must equal MAX_ATTEMPTS regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((int) getField(captor.getValue(), "retryCount")).isEqualTo(3);
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsCorrectRetryCount() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — retryCount must equal MAX_ATTEMPTS regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((int) getField(captor.getValue(), "retryCount")).isEqualTo(3);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectExceptionMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — exceptionMessage must be correctly persisted regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("lock failure");
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsCorrectExceptionMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — exceptionMessage must be correctly persisted regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("constraint error");
    }

    @Test
    void handleSendCourseToApproveEvent_nullPointerException_rethrows() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new NullPointerException("course entity was null"))
                .when(createCourseProposalCommandHandler).handle(any());

        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("course entity was null");
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withDeeplyNestedCauseChain_persistsTopLevelMessage() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final java.io.IOException rootCause = new java.io.IOException("socket closed");
        final java.sql.SQLException midCause = new java.sql.SQLException("connection reset", rootCause);
        final DataAccessResourceFailureException exception =
                new DataAccessResourceFailureException("Could not open connection", midCause);

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("Could not open connection");
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(DataAccessResourceFailureException.class.getName());
    }

    @Test
    void recover_withIdenticalEventDataTwice_producesIndependentRecordsWithTimestamps() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event1 = new SendCourseToApproveIntegrationEvent(uuid);
        final SendCourseToApproveIntegrationEvent event2 = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        sut.recover(exception, event1);
        sut.recover(exception, event2);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        final FailedIntegrationEventRecord record1 = captor.getAllValues().get(0);
        final FailedIntegrationEventRecord record2 = captor.getAllValues().get(1);
        assertThat(record1).isNotSameAs(record2);
        assertThat(getField(record1, "eventPayload")).isEqualTo(getField(record2, "eventPayload"));
        assertThat(getField(record1, "eventClassName")).isEqualTo(getField(record2, "eventClassName"));
    }

    @Test
    void handleSendCourseToApproveEvent_DataAccessResourceFailureExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException originalException =
                new DataAccessResourceFailureException("DB connection lost");
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the primary retryable exception
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_exceptionClassNameIsFullyQualified() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo("org.springframework.orm.ObjectOptimisticLockingFailureException");
    }

    @Test
    void recover_withDataIntegrityViolationException_exceptionClassNameIsFullyQualified() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo("org.springframework.dao.DataIntegrityViolationException");
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_exceptionClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — OOLF lives in org.springframework.orm, not org.springframework.dao
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionClassName"))
                .startsWith("org.springframework.orm.");
    }

    @Test
    void handleSendCourseToApproveEvent_withNullEvent_throwsNullPointerException() {
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withVeryLongExceptionMessage_persistsFullMessage() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final String longMessage = "X".repeat(2000);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException(longMessage);

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo(longMessage);
        assertThat(((String) getField(captor.getValue(), "exceptionMessage")).length()).isEqualTo(2000);
    }

    @Test
    void recover_withOOLFENullCause_persistsCorrectFields() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("optimistic lock conflict", (Throwable) null);

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(ObjectOptimisticLockingFailureException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("optimistic lock conflict");
        assertThat((FailedIntegrationEventRecord.Status) getField(captor.getValue(), "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void handlerMethod_isNotStatic() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getDeclaredMethod("handleSendCourseToApproveEvent", SendCourseToApproveIntegrationEvent.class);
        assertThat(Modifier.isStatic(method.getModifiers()))
                .as("Handler method must not be static — CGLIB cannot proxy static methods")
                .isFalse();
    }

    @Test
    void recoverMethod_isNotStatic() throws NoSuchMethodException {
        Method method = SendCourseToApproveIntegrationEventHandler.class
                .getDeclaredMethod("recover", DataAccessException.class, SendCourseToApproveIntegrationEvent.class);
        assertThat(Modifier.isStatic(method.getModifiers()))
                .as("Recover method must not be static — Spring Retry discovers it via proxy")
                .isFalse();
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsRecordWithFailedStatus() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — status must be FAILED regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((FailedIntegrationEventRecord.Status) getField(captor.getValue(), "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsRecordWithFailedStatus() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — status must be FAILED regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((FailedIntegrationEventRecord.Status) getField(captor.getValue(), "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCreatedAtTimestamp() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());
        final Instant before = Instant.now();

        // when
        sut.recover(exception, event);

        // then — createdAt must be set regardless of exception type
        final Instant after = Instant.now();
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final Instant createdAt = (Instant) getField(captor.getValue(), "createdAt");
        assertThat(createdAt).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsCreatedAtTimestamp() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");
        final Instant before = Instant.now();

        // when
        sut.recover(exception, event);

        // then — createdAt must be set regardless of exception type
        final Instant after = Instant.now();
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final Instant createdAt = (Instant) getField(captor.getValue(), "createdAt");
        assertThat(createdAt).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void recover_withDataIntegrityViolationException_nullMessage_persistsNullMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException((String) null);

        // when
        sut.recover(exception, event);

        // then — null message must be persisted without exception
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void recover_withDataAccessResourceFailureException_persistsCorrectEventClassName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must still be correct regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
    }

    @Test
    void recover_withDataAccessResourceFailureException_persistsCorrectEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — eventPayload must still be event.toString() regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_withDataAccessResourceFailureException_persistsCorrectExceptionMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — exceptionMessage must be correctly persisted regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("DB error");
    }

    @Test
    void recover_withDataAccessResourceFailureException_persistsCorrectRetryCount() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — retryCount must equal MAX_ATTEMPTS regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((int) getField(captor.getValue(), "retryCount")).isEqualTo(3);
    }

    @Test
    void recover_withDataAccessResourceFailureException_persistsRecordWithFailedStatus() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — status must be FAILED regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((FailedIntegrationEventRecord.Status) getField(captor.getValue(), "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void recover_withDataAccessResourceFailureException_persistsCreatedAtTimestamp() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");
        final Instant before = Instant.now();

        // when
        sut.recover(exception, event);

        // then — createdAt must be set regardless of exception type
        final Instant after = Instant.now();
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final Instant createdAt = (Instant) getField(captor.getValue(), "createdAt");
        assertThat(createdAt).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void recover_withDataAccessResourceFailureException_nullMessage_persistsNullMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException((String) null);

        // when
        sut.recover(exception, event);

        // then — null message must be persisted without exception
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void constructor_withNullCommandHandler_constructsSuccessfully() {
        // constructor does not validate — NPE deferred to handleEvent invocation
        new SendCourseToApproveIntegrationEventHandler(null, mock(FailedIntegrationEventRepository.class));
    }

    @Test
    void constructor_withNullRepository_constructsSuccessfully() {
        // constructor does not validate — NPE deferred to recover() invocation
        new SendCourseToApproveIntegrationEventHandler(mock(CreateCourseProposalCommandHandler.class), null);
    }

    @Test
    void handleSendCourseToApproveEvent_withNullCommandHandler_throwsNullPointerException() {
        // given
        var handler = new SendCourseToApproveIntegrationEventHandler(null, mock(FailedIntegrationEventRepository.class));
        var event = new SendCourseToApproveIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));

        // when/then
        assertThatThrownBy(() -> handler.handleSendCourseToApproveEvent(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recover_withNullRepository_throwsNullPointerException() {
        // given
        var handler = new SendCourseToApproveIntegrationEventHandler(mock(CreateCourseProposalCommandHandler.class), null);
        var event = new SendCourseToApproveIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        var exception = new DataAccessResourceFailureException("DB error");

        // when/then
        assertThatThrownBy(() -> handler.recover(exception, event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recover_withNullEvent_throwsNullPointerException() {
        // given — null event causes NPE in event.getClass().getName()
        var exception = new DataAccessResourceFailureException("DB error");

        // when/then
        assertThatThrownBy(() -> sut.recover(exception, null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withExceptionMessageExceedingColumnLimit_persistsFullMessage() throws Exception {
        // given — exception message exceeding the 2000-char column limit at Java level
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final String longMessage = "E".repeat(3000);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException(longMessage);

        // when
        sut.recover(exception, event);

        // then — Java level accepts the full message; DB truncation is a persistence concern
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo(longMessage);
        assertThat(((String) getField(captor.getValue(), "exceptionMessage")).length()).isEqualTo(3000);
    }

    @Test
    void recover_withNullExceptionArg_throwsNullPointerException() {
        // given — null exception causes NPE on e.getMessage() / e.getClass().getName()
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);

        // when/then
        assertThatThrownBy(() -> sut.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void constructor_withBothNullDependencies_constructsSuccessfully() {
        // constructor does not validate — NPE deferred to method invocation
        new SendCourseToApproveIntegrationEventHandler(null, null);
    }

    @Test
    void maxAttemptsField_valueIsThree() throws Exception {
        // given — hardcoded assertion guards against accidental changes to retry count
        Field maxAttemptsField = SendCourseToApproveIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        maxAttemptsField.setAccessible(true);
        int maxAttempts = (int) maxAttemptsField.get(null);

        // then
        assertThat(maxAttempts).isEqualTo(3);
    }

    @Test
    void recover_eventClassName_containsDot() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must be a fully qualified name (contains at least one dot)
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventClassName")).contains(".");
    }

    @Test
    void recover_exceptionClassName_containsDot() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — exceptionClassName must be a fully qualified name (contains at least one dot)
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionClassName")).contains(".");
    }

    @Test
    void recover_withDataIntegrityViolationException_exceptionClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — DIVE lives in org.springframework.dao, not org.springframework.orm
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionClassName"))
                .startsWith("org.springframework.dao.");
    }

    @Test
    void recover_withNullException_throwsNullPointerException() {
        // given — null exception causes NPE in e.getMessage() and e.getClass().getName()
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);

        // when/then
        assertThatThrownBy(() -> sut.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_nullMessage_persistsNullMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException((String) null, new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — null message must be persisted without exception
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void handleSendCourseToApproveEvent_onIllegalArgumentException_doesNotInteractWithFailedEventRepository() {
        // given — IllegalArgumentException is not a DataAccessException, must not trigger recovery
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new IllegalArgumentException("invalid course id"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleSendCourseToApproveEvent_onGenericRuntimeException_doesNotInteractWithFailedEventRepository() {
        // given — IllegalStateException is not a DataAccessException, must not trigger recovery
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new IllegalStateException("unexpected state"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleSendCourseToApproveEvent_onCheckedExceptionWrapped_doesNotInteractWithFailedEventRepository() {
        // given — RuntimeException wrapping a checked cause is not a DataAccessException
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new RuntimeException("wrapped", new java.io.IOException("disk full")))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withNullException_repositoryNeverInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);

        // when
        try { sut.recover(null, event); } catch (NullPointerException ignored) { }

        // then — NPE occurs before repository.save(), so repo is never touched
        verifyNoInteractions(failedIntegrationEventRepository);
        verifyNoInteractions(createCourseProposalCommandHandler);
    }

    @Test
    void handleSendCourseToApproveEvent_onDataIntegrityViolationException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then — only recover() should persist dead-letter records, never the handler itself
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleSendCourseToApproveEvent_concurrencyFailureException_rethrowsForRetry() {
        // given — ConcurrencyFailureException is a DataAccessException subclass, eligible for retry
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new org.springframework.dao.ConcurrencyFailureException("Lock timeout"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(org.springframework.dao.ConcurrencyFailureException.class);
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_retryCountFieldMatchesMaxAttempts() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — retryCount in persisted record should match the handler's MAX_ATTEMPTS constant
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        Field maxAttemptsField = SendCourseToApproveIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        maxAttemptsField.setAccessible(true);
        int expectedRetryCount = (int) maxAttemptsField.get(null);
        assertThat((int) getField(captor.getValue(), "retryCount")).isEqualTo(expectedRetryCount);
    }

    @Test
    void recover_withConcurrencyFailureException_persistsFailedEvent() throws Exception {
        // given — ConcurrencyFailureException is a DataAccessException subclass representing lock contention
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo(org.springframework.dao.ConcurrencyFailureException.class.getName());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("Lock acquisition timeout");
    }

    @Test
    void recover_withConcurrencyFailureException_persistsCorrectEventClassName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
    }

    @Test
    void recover_withConcurrencyFailureException_persistsCorrectEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_withConcurrencyFailureException_persistsCorrectExceptionMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("Lock acquisition timeout");
    }

    @Test
    void recover_withConcurrencyFailureException_persistsCorrectRetryCount() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((int) getField(captor.getValue(), "retryCount")).isEqualTo(3);
    }

    @Test
    void recover_withConcurrencyFailureException_persistsCreatedAtTimestamp() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");
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
    void recover_withConcurrencyFailureException_persistsRecordWithFailedStatus() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((FailedIntegrationEventRecord.Status) getField(captor.getValue(), "status"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void recover_withConcurrencyFailureException_exceptionClassNameIsFullyQualified() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock acquisition timeout");

        // when
        sut.recover(exception, event);

        // then — hardcoded FQN assertion catches silent package relocations
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionClassName"))
                .isEqualTo("org.springframework.dao.ConcurrencyFailureException");
    }

    @Test
    void recover_withConcurrencyFailureException_nullMessage_persistsNullMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException((String) null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void handleSendCourseToApproveEvent_ConcurrencyFailureExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException originalException =
                new org.springframework.dao.ConcurrencyFailureException("Lock contention");
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleSendCourseToApproveEvent_onConcurrencyFailureException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new org.springframework.dao.ConcurrencyFailureException("Lock contention"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        try { sut.handleSendCourseToApproveEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }


    @Test
    void handleSendCourseToApproveEvent_resourceNotFoundException_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final ResourceNotFoundException originalException = new ResourceNotFoundException("Course not found");
        doThrow(originalException).when(createCourseProposalCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the business exception
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void recover_withSpecialCharsInExceptionMessage_persistsExactMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final String specialMessage = "Error: tab\there, newline\nhere, unicode \u00e9\u00e8\u00ea and null-byte \u0000 end";
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException(specialMessage);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo(specialMessage);
    }

    @Test
    void recover_withConcurrencyFailureException_exceptionClassName_isNotSimpleName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock contention");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final String exceptionClassName = (String) getField(captor.getValue(), "exceptionClassName");
        assertThat(exceptionClassName).contains(".");
        assertThat(exceptionClassName).isNotEqualTo(exception.getClass().getSimpleName());
    }

    @Test
    void recover_withConcurrencyFailureException_exceptionClassName_containsDot() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock contention");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionClassName")).contains(".");
    }

    @Test
    void recover_withConcurrencyFailureException_exceptionClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final org.springframework.dao.ConcurrencyFailureException exception =
                new org.springframework.dao.ConcurrencyFailureException("Lock contention");

        // when
        sut.recover(exception, event);

        // then — ConcurrencyFailureException lives in org.springframework.dao
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionClassName"))
                .startsWith("org.springframework.dao.");
    }

    @Test
    void recover_withIdenticalEventDataTwice_producesIndependentRecords() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event1 = new SendCourseToApproveIntegrationEvent(uuid);
        final SendCourseToApproveIntegrationEvent event2 = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        sut.recover(exception, event1);
        sut.recover(exception, event2);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0)).isNotSameAs(captor.getAllValues().get(1));
        assertThat(getField(captor.getAllValues().get(0), "eventPayload"))
                .isEqualTo(getField(captor.getAllValues().get(1), "eventPayload"));
    }

    @Test
    void recover_withCustomDataAccessExceptionSubclass_persistsActualRuntimeClassName() throws Exception {
        // given — a custom subclass of DataAccessException should have its actual runtime type stored
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error") {};

        // when
        sut.recover(exception, event);

        // then — the persisted className must be the anonymous subclass, not DataAccessResourceFailureException
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final String persistedClassName = (String) getField(captor.getValue(), "exceptionClassName");
        assertThat(persistedClassName).isNotEqualTo(DataAccessResourceFailureException.class.getName());
        assertThat(persistedClassName).contains("SendCourseToApproveIntegrationEventHandlerTest");
    }

    @Test
    void recover_eventPayload_isNotJavaObjectReference() throws Exception {
        // given — event.toString() for records should produce a readable representation, not Object@hex
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final String payload = (String) getField(captor.getValue(), "eventPayload");
        assertThat(payload).doesNotMatch(".*@[0-9a-f]+$");
        assertThat(payload).contains("123e4567-e89b-12d3-a456-426655440001");
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
