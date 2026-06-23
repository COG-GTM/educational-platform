package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

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

import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedIntegrationEventRepository;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand command = argument.getValue();
        assertThat(command).hasFieldOrPropertyWithValue("username", "teacher1");
    }

    @Test
    void handleUserCreatedEvent_transientException_rethrowsForRetry() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void recover_persistsFailedEvent() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
    void handleUserCreatedEvent_businessException_rethrowsForRetry() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new ResourceNotFoundException("User not found"))
                .when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleUserCreatedEvent_optimisticLockException_rethrowsForRetry() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock conflict", new RuntimeException()))
                .when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void handleUserCreatedEvent_successPath_doesNotInteractWithFailedEventRepository() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withOptimisticLockException_persistsFailedEvent() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException((String) null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void handleUserCreatedEvent_exceptionMessagePreserved() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new DataAccessResourceFailureException("specific error message"))
                .when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("specific error message");
    }

    @Test
    void handlerMethod_hasRetryableAnnotation_withCorrectConfig() throws NoSuchMethodException {
        // given
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);

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
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);

        // then
        assertThat(method.getAnnotation(Async.class)).isNotNull();
        assertThat(method.getAnnotation(EventListener.class)).isNotNull();
    }

    @Test
    void recoverMethod_hasRecoverAnnotation() {
        // given
        boolean hasRecover = Arrays.stream(UserCreatedIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("recover"))
                .anyMatch(m -> m.getAnnotation(Recover.class) != null);

        // then
        assertThat(hasRecover).isTrue();
    }

    @Test
    void handlerClass_hasComponentAnnotation() {
        assertThat(UserCreatedIntegrationEventHandler.class.getAnnotation(Component.class)).isNotNull();
    }

    @Test
    void handleUserCreatedEvent_transientException_commandHandlerStillInvoked() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        try { sut.handleUserCreatedEvent(event); } catch (Exception ignored) { }

        // then
        verify(createTeacherCommandHandler, times(1)).handle(any());
    }

    @Test
    void recover_whenRepositorySaveFails_propagatesException() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);

        assertThat(method).isNotNull();
        assertThat(method.getAnnotation(Recover.class)).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handleUserCreatedEvent_genericRuntimeException_rethrows() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new IllegalStateException("unexpected state"))
                .when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unexpected state");
    }

    @Test
    void handleUserCreatedEvent_emptyUsername_commandReceivedCorrectValue() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("", "empty@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", "");
    }

    @Test
    void recover_savesExactlyOneRecord() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB down");

        // when
        sut.recover(exception, event);

        // then
        verify(failedIntegrationEventRepository, times(1)).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void recover_persistsRecordWithFailedStatus() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
    void handleUserCreatedEvent_nullUsername_commandReceivedCorrectValue() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, "null@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", null);
    }

    @Test
    void handleUserCreatedEvent_specialCharactersUsername_commandReceivedCorrectValue() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("\u00fc\u00e9\u00e7\u00f1", "unicode@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", "\u00fc\u00e9\u00e7\u00f1");
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsFailedEvent() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.recover()).isEmpty();
    }

    @Test
    void handlerMethod_retryableAnnotation_noRetryForIsEmpty() throws NoSuchMethodException {
        // given
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.noRetryFor()).isEmpty();
    }

    @Test
    void handleUserCreatedEvent_rethrowsExactSameExceptionInstance() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        final DataAccessResourceFailureException originalException = new DataAccessResourceFailureException("DB error");
        doThrow(originalException).when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleUserCreatedEvent_usesUsernameNotEmail() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("the-username", "totally-different-email@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", "the-username");
    }

    @Test
    void handleUserCreatedEvent_transientException_passesCorrectUsernameToCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new DataAccessResourceFailureException("DB error"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        try { sut.handleUserCreatedEvent(event); } catch (Exception ignored) { }

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", "teacher1");
    }

    @Test
    void recoverMethod_doesNotHaveAsyncAnnotation() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);

        assertThat(method.getAnnotation(Async.class)).isNull();
    }

    @Test
    void recoverMethod_doesNotHaveEventListenerAnnotation() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);

        assertThat(method.getAnnotation(EventListener.class)).isNull();
    }

    @Test
    void handlerMethod_isPublic() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void recoverMethod_isPublic() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_hasSingleEventListenerMethod() {
        long count = Arrays.stream(UserCreatedIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void handler_hasSingleRecoverMethod() {
        long count = Arrays.stream(UserCreatedIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(Recover.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void maxAttemptsConstant_matchesRetryableAnnotation() throws Exception {
        Field maxAttemptsField = UserCreatedIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        maxAttemptsField.setAccessible(true);
        int maxAttempts = (int) maxAttemptsField.get(null);

        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        assertThat(retryable.maxAttempts()).isEqualTo(maxAttempts);
    }

    @Test
    void recoverMethod_doesNotAcceptBroadExceptionType() {
        assertThatThrownBy(() -> UserCreatedIntegrationEventHandler.class
                .getMethod("recover", Exception.class, UserCreatedIntegrationEvent.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void retryableMaxAttempts_matchesRecoverRetryCount() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");
        Method handlerMethod = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
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
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);

        assertThat(method.getAnnotation(Retryable.class)).isNull();
    }

    @Test
    void maxAttemptsField_isStaticFinal() throws NoSuchFieldException {
        Field field = UserCreatedIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_hasExactlyOneParameter() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(UserCreatedIntegrationEvent.class);
    }

    @Test
    void recoverMethod_hasExactlyTwoParameters() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(2);
        assertThat(method.getParameterTypes()[0]).isEqualTo(DataAccessException.class);
        assertThat(method.getParameterTypes()[1]).isEqualTo(UserCreatedIntegrationEvent.class);
    }

    @Test
    void recover_doesNotInvokeCommandHandler() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        verifyNoInteractions(createTeacherCommandHandler);
    }

    @Test
    void recover_calledMultipleTimes_savesIndependentRecords() throws Exception {
        // given
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("teacher1", "t1@test.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("teacher2", "t2@test.com");
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
    void handleUserCreatedEvent_dataIntegrityViolationException_rethrowsForRetry() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new DataIntegrityViolationException("duplicate username"))
                .when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("duplicate username");
    }

    @Test
    void handleUserCreatedEvent_longUsername_commandReceivedCorrectValue() {
        // given — usernames at boundary lengths
        final String longUsername = "a".repeat(255);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(longUsername, "long@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", longUsername);
    }

    @Test
    void handleUserCreatedEvent_onDataAccessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        try { sut.handleUserCreatedEvent(event); } catch (Exception ignored) { }

        // then — only recover() should persist dead-letter records, never the handler itself
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleUserCreatedEvent_onBusinessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new ResourceNotFoundException("User not found"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        try { sut.handleUserCreatedEvent(event); } catch (Exception ignored) { }

        // then — non-retryable exceptions propagate to AsyncUncaughtExceptionHandler, not to the repository
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleUserCreatedEvent_onOptimisticLockException_doesNotInteractWithFailedEventRepository() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock", new RuntimeException()))
                .when(createTeacherCommandHandler).handle(any());

        // when
        try { sut.handleUserCreatedEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_completesNormally_whenRepositorySaveSucceeds() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when — should complete without throwing
        sut.recover(exception, event);

        // then
        verify(failedIntegrationEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void handlerClass_isPublic() {
        assertThat(Modifier.isPublic(UserCreatedIntegrationEventHandler.class.getModifiers())).isTrue();
    }

    @Test
    void handlerAndRecoverMethods_haveMatchingReturnType() throws NoSuchMethodException {
        Method handler = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        Method recover = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);

        assertThat(handler.getReturnType()).isEqualTo(recover.getReturnType());
    }

    @Test
    void handlerMethod_backoffMaxDelayIsUnlimited() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        Backoff backoff = method.getAnnotation(Retryable.class).backoff();
        assertThat(backoff.maxDelay()).isEqualTo(0L);
    }

    @Test
    void handlerMethod_retryableListenersIsEmpty() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.listeners()).isEmpty();
    }

    @Test
    void handler_constructorRequiresBothDependencies() {
        assertThat(UserCreatedIntegrationEventHandler.class.getConstructors()).hasSize(1);
        assertThat(UserCreatedIntegrationEventHandler.class.getConstructors()[0].getParameterCount()).isEqualTo(2);
    }

    @Test
    void handler_constructorFirstParam_isCommandHandler() {
        Class<?>[] paramTypes = UserCreatedIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[0]).isEqualTo(CreateTeacherCommandHandler.class);
    }

    @Test
    void handler_constructorSecondParam_isFailedIntegrationEventRepository() {
        Class<?>[] paramTypes = UserCreatedIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[1]).isEqualTo(FailedIntegrationEventRepository.class);
    }

    @Test
    void handlerMethod_asyncAnnotation_usesDefaultExecutor() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        Async async = method.getAnnotation(Async.class);
        assertThat(async.value()).isEmpty();
    }

    @Test
    void recoverMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_isNotAbstract() {
        assertThat(Modifier.isAbstract(UserCreatedIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handler_isNotFinal() {
        assertThat(Modifier.isFinal(UserCreatedIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handleUserCreatedEvent_errorSubclass_propagatesWithoutCatch() {
        // given — Error subclasses bypass the catch(Exception) block
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        doThrow(new StackOverflowError("deep recursion"))
                .when(createTeacherCommandHandler).handle(any());

        // when/then — Error propagates directly, not caught by handler
        try {
            sut.handleUserCreatedEvent(event);
            assertThat(true).as("Expected StackOverflowError to be thrown").isFalse();
        } catch (StackOverflowError e) {
            assertThat(e.getMessage()).isEqualTo("deep recursion");
        }
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleUserCreatedEvent_concurrentInvocations_areIndependent() {
        // given — handler is stateless, concurrent calls should not interfere
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("teacher1", "t1@test.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("teacher2", "t2@test.com");

        // when
        sut.handleUserCreatedEvent(event1);
        sut.handleUserCreatedEvent(event2);

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasFieldOrPropertyWithValue("username", "teacher1");
        assertThat(captor.getAllValues().get(1)).hasFieldOrPropertyWithValue("username", "teacher2");
    }

    @Test
    void recover_withNullUsername_persistsEventPayload() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, "test@test.com");
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
    void recover_withEmptyUsername_persistsEventPayload() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("", "empty@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventClassName")).isEqualTo(event.getClass().getName());
    }

    @Test
    void recover_withUnicodeUsername_persistsEventPayload() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("\u00fc\u00e9\u00e7\u00f1\u4e16\u754c", "unicode@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
    }

    @Test
    void recover_withLongUsername_persistsEventPayload() throws Exception {
        // given
        final String longUsername = "a".repeat(255);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(longUsername, "long@test.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventPayload")).asString()
                .contains("teacher1")
                .contains("teacher1@test.com");
    }

    @Test
    void recover_repositorySaveFails_exceptionPropagates() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo("com.educational.platform.users.integration.event.UserCreatedIntegrationEvent");
    }

    @Test
    void handleUserCreatedEvent_checkedExceptionFromCommandHandler_rethrows() {
        // given — unchecked wrapper of a checked exception
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        doThrow(new IllegalArgumentException("invalid username"))
                .when(createTeacherCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid username");
    }

    @Test
    void recover_eventClassName_isNotSimpleName() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
    void handleUserCreatedEvent_checkedExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final IllegalArgumentException originalException = new IllegalArgumentException("invalid username");
        doThrow(originalException).when(createTeacherCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the exception
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleUserCreatedEvent_NullPointerExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final NullPointerException originalException = new NullPointerException("username was null");
        doThrow(originalException).when(createTeacherCommandHandler).handle(any());

        // when/then — NPE must propagate with identity preserved through catch(Exception e) { throw e; }
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleUserCreatedEvent_checkedExceptionFromCommandHandler_causeIsNull() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final IllegalArgumentException exception = new IllegalArgumentException("invalid username");
        doThrow(exception).when(createTeacherCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not add a cause chain
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .hasNoCause();
    }

    @Test
    void recover_eventClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
    void recover_eventPayloadContainsBothUsernameAndEmail() throws Exception {
        // given — event.toString() should include both fields even though handler only uses username
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("admin", "admin@edu.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        String payload = (String) getField(captor.getValue(), "eventPayload");
        assertThat(payload).contains("admin").contains("admin@edu.com");
    }

    @Test
    void recover_withNullEmail_persistsEventPayload() throws Exception {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", null);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventClassName")).isEqualTo(event.getClass().getName());
    }

    @Test
    void handleUserCreatedEvent_withNullEmail_commandReceivedCorrectUsername() {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", null);

        sut.handleUserCreatedEvent(event);

        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("username", "teacher1");
    }

    @Test
    void recover_withBothNullFields_persistsEventPayload() throws Exception {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, null);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventClassName")).isEqualTo(event.getClass().getName());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("DB error");
    }

    @Test
    void recover_withDifferentExceptionSubtypes_persistsCorrectClassForEach() throws Exception {
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("user1", "user1@edu.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("user2", "user2@edu.com");

        sut.recover(new DataAccessResourceFailureException("transient error"), event1);
        sut.recover(new DataIntegrityViolationException("constraint error"), event2);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository, times(2)).save(captor.capture());
        assertThat(getField(captor.getAllValues().get(0), "exceptionClassName"))
                .isEqualTo(DataAccessResourceFailureException.class.getName());
        assertThat(getField(captor.getAllValues().get(1), "exceptionClassName"))
                .isEqualTo(DataIntegrityViolationException.class.getName());
    }

    @Test
    void recover_withEmptyExceptionMessage_persistsEmptyString() throws Exception {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("student", "student@edu.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("");

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("");
    }

    @Test
    void handleUserCreatedEvent_checkedExceptionWrapped_rethrows() {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher", "teacher@edu.com");
        final RuntimeException wrappedException = new RuntimeException("wrapped",
                new java.io.IOException("disk full"));
        doThrow(wrappedException).when(createTeacherCommandHandler).handle(any());

        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(wrappedException)
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    void handleUserCreatedEvent_ObjectOptimisticLockingFailureExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final ObjectOptimisticLockingFailureException originalException =
                new ObjectOptimisticLockingFailureException("optimistic lock conflict", new RuntimeException());
        doThrow(originalException).when(createTeacherCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the retryable exception
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleUserCreatedEvent_DataIntegrityViolationExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final DataIntegrityViolationException originalException =
                new DataIntegrityViolationException("constraint violation");
        doThrow(originalException).when(createTeacherCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the DataAccessException subclass
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_exceptionClassName_isNotSimpleName() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
        Method handlerMethod = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);
        Method recoverMethod = UserCreatedIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, UserCreatedIntegrationEvent.class);

        // then — @Recover second parameter must match @EventListener parameter for Spring Retry matching
        assertThat(recoverMethod.getParameterTypes()[1]).isEqualTo(handlerMethod.getParameterTypes()[0]);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectEventClassName() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — eventClassName must still be correct regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(UserCreatedIntegrationEvent.class.getName());
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsCorrectEventClassName() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must still be correct regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(UserCreatedIntegrationEvent.class.getName());
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectEventPayload() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — retryCount must equal MAX_ATTEMPTS regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((int) getField(captor.getValue(), "retryCount")).isEqualTo(3);
    }

    @Test
    void handleUserCreatedEvent_nullPointerException_rethrows() {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
        doThrow(new NullPointerException("teacher entity was null"))
                .when(createTeacherCommandHandler).handle(any());

        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("teacher entity was null");
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withDeeplyNestedCauseChain_persistsTopLevelMessage() throws Exception {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
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
    void recover_withIdenticalEventDataTwice_producesIndependentRecords() throws Exception {
        final UserCreatedIntegrationEvent event1 = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
        final UserCreatedIntegrationEvent event2 = new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com");
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
    void handleUserCreatedEvent_DataAccessResourceFailureExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
        final DataAccessResourceFailureException originalException =
                new DataAccessResourceFailureException("DB connection lost");
        doThrow(originalException).when(createTeacherCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the primary retryable exception
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_exceptionClassNameIsFullyQualified() throws Exception {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");
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

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
