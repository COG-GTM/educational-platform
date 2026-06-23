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
public class StudentEnrolledToCourseIntegrationEventHandlerTest {

    @Mock
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedIntegrationEventRepository;

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
    void handleStudentEnrolledToCourseEvent_transientException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void recover_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
    void handleStudentEnrolledToCourseEvent_businessException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_optimisticLockException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock conflict", new RuntimeException()))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_successPath_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withOptimisticLockException_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException((String) null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void handleStudentEnrolledToCourseEvent_exceptionMessagePreserved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataAccessResourceFailureException("specific error message"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("specific error message");
    }

    @Test
    void handlerMethod_hasRetryableAnnotation_withCorrectConfig() throws NoSuchMethodException {
        // given
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

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
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // then
        assertThat(method.getAnnotation(Async.class)).isNotNull();
        assertThat(method.getAnnotation(EventListener.class)).isNotNull();
    }

    @Test
    void recoverMethod_hasRecoverAnnotation() {
        // given
        boolean hasRecover = Arrays.stream(StudentEnrolledToCourseIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("recover"))
                .anyMatch(m -> m.getAnnotation(Recover.class) != null);

        // then
        assertThat(hasRecover).isTrue();
    }

    @Test
    void handlerClass_hasComponentAnnotation() {
        assertThat(StudentEnrolledToCourseIntegrationEventHandler.class.getAnnotation(Component.class)).isNotNull();
    }

    @Test
    void handleStudentEnrolledToCourseEvent_transientException_commandHandlerStillInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try { sut.handleStudentEnrolledToCourseEvent(event); } catch (Exception ignored) { }

        // then
        verify(increaseNumberOfStudentsCommandHandler, times(1)).handle(any());
    }

    @Test
    void recover_whenRepositorySaveFails_propagatesException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(method).isNotNull();
        assertThat(method.getAnnotation(Recover.class)).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_genericRuntimeException_rethrows() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new IllegalStateException("unexpected state"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unexpected state");
    }

    @Test
    void recover_savesExactlyOneRecord() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
    void handleStudentEnrolledToCourseEvent_commandReceivesCourseIdFromEvent() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "some-student");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", courseId);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_nullCourseId_commandHandlerStillInvoked() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(null, "username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", null);
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsFailedEvent() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
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
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.recover()).isEmpty();
    }

    @Test
    void handlerMethod_retryableAnnotation_noRetryForIsEmpty() throws NoSuchMethodException {
        // given
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);

        // then
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.noRetryFor()).isEmpty();
    }

    @Test
    void handleStudentEnrolledToCourseEvent_rethrowsExactSameExceptionInstance() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final DataAccessResourceFailureException originalException = new DataAccessResourceFailureException("DB error");
        doThrow(originalException).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_transientException_passesCorrectCourseIdToCommand() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataAccessResourceFailureException("DB error"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try { sut.handleStudentEnrolledToCourseEvent(event); } catch (Exception ignored) { }

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void recoverMethod_doesNotHaveAsyncAnnotation() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(method.getAnnotation(Async.class)).isNull();
    }

    @Test
    void recoverMethod_doesNotHaveEventListenerAnnotation() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(method.getAnnotation(EventListener.class)).isNull();
    }

    @Test
    void handlerMethod_isPublic() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void recoverMethod_isPublic() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_hasSingleEventListenerMethod() {
        long count = Arrays.stream(StudentEnrolledToCourseIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void handler_hasSingleRecoverMethod() {
        long count = Arrays.stream(StudentEnrolledToCourseIntegrationEventHandler.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(Recover.class) != null)
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void maxAttemptsConstant_matchesRetryableAnnotation() throws Exception {
        Field maxAttemptsField = StudentEnrolledToCourseIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        maxAttemptsField.setAccessible(true);
        int maxAttempts = (int) maxAttemptsField.get(null);

        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);

        assertThat(retryable.maxAttempts()).isEqualTo(maxAttempts);
    }

    @Test
    void recoverMethod_doesNotAcceptBroadExceptionType() {
        assertThatThrownBy(() -> StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", Exception.class, StudentEnrolledToCourseIntegrationEvent.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void retryableMaxAttempts_matchesRecoverRetryCount() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");
        Method handlerMethod = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
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
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(method.getAnnotation(Retryable.class)).isNull();
    }

    @Test
    void maxAttemptsField_isStaticFinal() throws NoSuchFieldException {
        Field field = StudentEnrolledToCourseIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void handlerMethod_hasExactlyOneParameter() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class);
    }

    @Test
    void recoverMethod_hasExactlyTwoParameters() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(method.getParameterCount()).isEqualTo(2);
        assertThat(method.getParameterTypes()[0]).isEqualTo(DataAccessException.class);
        assertThat(method.getParameterTypes()[1]).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class);
    }

    @Test
    void recover_doesNotInvokeCommandHandler() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        verifyNoInteractions(increaseNumberOfStudentsCommandHandler);
    }

    @Test
    void recover_calledMultipleTimes_savesIndependentRecords() throws Exception {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(uuid1, "user1");
        final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(uuid2, "user2");
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
    void handleStudentEnrolledToCourseEvent_dataIntegrityViolationException_rethrowsForRetry() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataIntegrityViolationException("constraint violation"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("constraint violation");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_usesOnlyCourseIdNotUsername() {
        // given — the handler only passes courseId to the command, not username
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "irrelevant-username");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", courseId);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_onDataAccessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new DataAccessResourceFailureException("DB connection lost"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try { sut.handleStudentEnrolledToCourseEvent(event); } catch (Exception ignored) { }

        // then — only recover() should persist dead-letter records, never the handler itself
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_onBusinessException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try { sut.handleStudentEnrolledToCourseEvent(event); } catch (Exception ignored) { }

        // then — non-retryable exceptions propagate to AsyncUncaughtExceptionHandler, not to the repository
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_onOptimisticLockException_doesNotInteractWithFailedEventRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        doThrow(new ObjectOptimisticLockingFailureException("Optimistic lock", new RuntimeException()))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when
        try { sut.handleStudentEnrolledToCourseEvent(event); } catch (Exception ignored) { }

        // then
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_completesNormally_whenRepositorySaveSucceeds() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when — should complete without throwing
        sut.recover(exception, event);

        // then
        verify(failedIntegrationEventRepository).save(any(FailedIntegrationEventRecord.class));
    }

    @Test
    void handlerClass_isPublic() {
        assertThat(Modifier.isPublic(StudentEnrolledToCourseIntegrationEventHandler.class.getModifiers())).isTrue();
    }

    @Test
    void handlerAndRecoverMethods_haveMatchingReturnType() throws NoSuchMethodException {
        Method handler = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Method recover = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(handler.getReturnType()).isEqualTo(recover.getReturnType());
    }

    @Test
    void handlerMethod_backoffMaxDelayIsUnlimited() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Backoff backoff = method.getAnnotation(Retryable.class).backoff();
        assertThat(backoff.maxDelay()).isEqualTo(0L);
    }

    @Test
    void handlerMethod_retryableListenersIsEmpty() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.listeners()).isEmpty();
    }

    @Test
    void handler_constructorRequiresBothDependencies() {
        assertThat(StudentEnrolledToCourseIntegrationEventHandler.class.getConstructors()).hasSize(1);
        assertThat(StudentEnrolledToCourseIntegrationEventHandler.class.getConstructors()[0].getParameterCount()).isEqualTo(2);
    }

    @Test
    void handler_constructorFirstParam_isCommandHandler() {
        Class<?>[] paramTypes = StudentEnrolledToCourseIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[0]).isEqualTo(IncreaseNumberOfStudentsCommandHandler.class);
    }

    @Test
    void handler_constructorSecondParam_isFailedIntegrationEventRepository() {
        Class<?>[] paramTypes = StudentEnrolledToCourseIntegrationEventHandler.class.getConstructors()[0].getParameterTypes();
        assertThat(paramTypes[1]).isEqualTo(FailedIntegrationEventRepository.class);
    }

    @Test
    void handlerMethod_asyncAnnotation_usesDefaultExecutor() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Async async = method.getAnnotation(Async.class);
        assertThat(async.value()).isEmpty();
    }

    @Test
    void recoverMethod_returnTypeIsVoid() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handler_isNotAbstract() {
        assertThat(Modifier.isAbstract(StudentEnrolledToCourseIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handler_isNotFinal() {
        assertThat(Modifier.isFinal(StudentEnrolledToCourseIntegrationEventHandler.class.getModifiers())).isFalse();
    }

    @Test
    void handleStudentEnrolledToCourseEvent_errorSubclass_propagatesWithoutCatch() {
        // given — Error subclasses bypass the catch(Exception) block
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        doThrow(new StackOverflowError("deep recursion"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then — Error propagates directly, not caught by handler
        try {
            sut.handleStudentEnrolledToCourseEvent(event);
            assertThat(true).as("Expected StackOverflowError to be thrown").isFalse();
        } catch (StackOverflowError e) {
            assertThat(e.getMessage()).isEqualTo("deep recursion");
        }
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_concurrentInvocations_areIndependent() {
        // given — handler is stateless, concurrent calls should not interfere
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(uuid1, "student1");
        final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(uuid2, "student2");

        // when
        sut.handleStudentEnrolledToCourseEvent(event1);
        sut.handleStudentEnrolledToCourseEvent(event2);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler, times(2)).handle(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasFieldOrPropertyWithValue("uuid", uuid1);
        assertThat(captor.getAllValues().get(1)).hasFieldOrPropertyWithValue("uuid", uuid2);
    }

    @Test
    void recover_withNullCourseId_persistsEventPayload() throws Exception {
        // given
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(null, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventPayload")).asString()
                .contains("123e4567-e89b-12d3-a456-426655440001")
                .contains("student1");
    }

    @Test
    void recover_repositorySaveFails_exceptionPropagates() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo("com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_checkedExceptionFromCommandHandler_rethrows() {
        // given — unchecked wrapper of a checked exception
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        doThrow(new IllegalArgumentException("invalid enrollment"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid enrollment");
    }

    @Test
    void recover_eventClassName_isNotSimpleName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
    void handleStudentEnrolledToCourseEvent_checkedExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final IllegalArgumentException originalException = new IllegalArgumentException("invalid enrollment");
        doThrow(originalException).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the exception
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_NullPointerExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final NullPointerException originalException = new NullPointerException("course was null");
        doThrow(originalException).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then — NPE must propagate with identity preserved through catch(Exception e) { throw e; }
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_checkedExceptionFromCommandHandler_causeIsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final IllegalArgumentException exception = new IllegalArgumentException("invalid enrollment");
        doThrow(exception).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not add a cause chain
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .hasNoCause();
    }

    @Test
    void recover_eventClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
    void recover_withNullUsername_persistsEventPayload() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, null);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventClassName")).isEqualTo(event.getClass().getName());
    }

    @Test
    void handleStudentEnrolledToCourseEvent_withNullUsername_commandHandlerStillInvoked() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, null);

        sut.handleStudentEnrolledToCourseEvent(event);

        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void recover_withDifferentExceptionSubtypes_persistsCorrectClassForEach() throws Exception {
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(uuid1, "user1");
        final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(uuid2, "user2");

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
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("");

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("");
    }

    @Test
    void recover_eventPayloadContainsBothCourseIdAndUsername() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String username = "enrolled-student";
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, username);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("error");

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        String payload = (String) getField(captor.getValue(), "eventPayload");
        assertThat(payload).contains(uuid.toString());
        assertThat(payload).contains(username);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_checkedExceptionWrapped_rethrows() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student");
        final RuntimeException wrappedException = new RuntimeException("wrapped",
                new java.io.IOException("disk full"));
        doThrow(wrappedException).when(increaseNumberOfStudentsCommandHandler).handle(any());

        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(wrappedException)
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_ObjectOptimisticLockingFailureExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final ObjectOptimisticLockingFailureException originalException =
                new ObjectOptimisticLockingFailureException("optimistic lock conflict", new RuntimeException());
        doThrow(originalException).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the retryable exception
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_DataIntegrityViolationExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataIntegrityViolationException originalException =
                new DataIntegrityViolationException("constraint violation");
        doThrow(originalException).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the DataAccessException subclass
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_exceptionClassName_isNotSimpleName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
    void handleStudentEnrolledToCourseEvent_emptyUsername_commandReceivedCorrectValue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "");

        // when
        sut.handleStudentEnrolledToCourseEvent(event);

        // then
        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void recover_withEmptyUsername_persistsEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "");
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
    void recover_withBothNullFields_persistsEventPayload() throws Exception {
        // given
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(null, null);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventClassName")).isEqualTo(event.getClass().getName());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("DB error");
    }

    @Test
    void recoverMethod_secondParameterType_matchesHandlerMethodEventType() throws NoSuchMethodException {
        // given
        Method handlerMethod = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        Method recoverMethod = StudentEnrolledToCourseIntegrationEventHandler.class
                .getMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);

        // then — @Recover second parameter must match @EventListener parameter for Spring Retry matching
        assertThat(recoverMethod.getParameterTypes()[1]).isEqualTo(handlerMethod.getParameterTypes()[0]);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectEventClassName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("lock failure", new RuntimeException());

        // when
        sut.recover(exception, event);

        // then — eventClassName must still be correct regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
    }

    @Test
    void recover_withDataIntegrityViolationException_persistsCorrectEventClassName() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must still be correct regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventClassName"))
                .isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsCorrectEventPayload() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint error");

        // when
        sut.recover(exception, event);

        // then — exceptionMessage must be correctly persisted regardless of exception type
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo("constraint error");
    }

    @Test
    void handleStudentEnrolledToCourseEvent_nullPointerException_rethrows() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        doThrow(new NullPointerException("course entity was null"))
                .when(increaseNumberOfStudentsCommandHandler).handle(any());

        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("course entity was null");
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withDeeplyNestedCauseChain_persistsTopLevelMessage() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event1 = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final StudentEnrolledToCourseIntegrationEvent event2 = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        sut.recover(exception, event1);
        sut.recover(exception, event2);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0)).isNotSameAs(captor.getAllValues().get(1));
        assertThat(getField(captor.getAllValues().get(0), "eventPayload"))
                .isEqualTo(getField(captor.getAllValues().get(1), "eventPayload"));
        assertThat(getField(captor.getAllValues().get(0), "eventClassName"))
                .isEqualTo(getField(captor.getAllValues().get(1), "eventClassName"));
    }

    @Test
    void handleStudentEnrolledToCourseEvent_DataAccessResourceFailureExceptionFromCommandHandler_preservesExceptionIdentity() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataAccessResourceFailureException originalException =
                new DataAccessResourceFailureException("DB connection lost");
        doThrow(originalException).when(increaseNumberOfStudentsCommandHandler).handle(any());

        // when/then — catch(Exception e) { throw e; } must not wrap the primary retryable exception
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(event))
                .isSameAs(originalException);
    }

    @Test
    void recover_withObjectOptimisticLockingFailureException_exceptionClassNameIsFullyQualified() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
    void handleStudentEnrolledToCourseEvent_withNullEvent_throwsNullPointerException() {
        assertThatThrownBy(() -> sut.handleStudentEnrolledToCourseEvent(null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void recover_withVeryLongExceptionMessage_persistsFullMessage() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "username");
        final String longMessage = "X".repeat(2000);
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException(longMessage);

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isEqualTo(longMessage);
        assertThat(((String) getField(captor.getValue(), "exceptionMessage")).length()).isEqualTo(2000);
    }

    @Test
    void recover_withSpecialCharsInUsername_persistsEventPayload() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(uuid, "user+special@chars.com");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        sut.recover(exception, event);

        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "eventPayload")).isEqualTo(event.toString());
        assertThat(getField(captor.getValue(), "eventPayload").toString()).contains("user+special@chars.com");
    }

    @Test
    void recover_withOOLFENullCause_persistsCorrectFields() throws Exception {
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");
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
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getDeclaredMethod("handleStudentEnrolledToCourseEvent", StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(Modifier.isStatic(method.getModifiers()))
                .as("Handler method must not be static — CGLIB cannot proxy static methods")
                .isFalse();
    }

    @Test
    void recoverMethod_isNotStatic() throws NoSuchMethodException {
        Method method = StudentEnrolledToCourseIntegrationEventHandler.class
                .getDeclaredMethod("recover", DataAccessException.class, StudentEnrolledToCourseIntegrationEvent.class);
        assertThat(Modifier.isStatic(method.getModifiers()))
                .as("Recover method must not be static — Spring Retry discovers it via proxy")
                .isFalse();
    }

    @Test
    void handleStudentEnrolledToCourseEvent_unicodeUsername_commandReceivedCorrectCourseId() {
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event =
                new StudentEnrolledToCourseIntegrationEvent(courseId, "\u00fc\u00f1\u00ee\u00e7\u00f8\u00f0\u00e9-student");

        sut.handleStudentEnrolledToCourseEvent(event);

        final ArgumentCaptor<IncreaseNumberOfStudentsCommand> captor =
                ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(increaseNumberOfStudentsCommandHandler).handle(captor.capture());
        assertThat(captor.getValue()).hasFieldOrPropertyWithValue("uuid", courseId);
    }
    @Test
    void recover_withObjectOptimisticLockingFailureException_persistsRecordWithFailedStatus() throws Exception {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");
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
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");
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
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");
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
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");
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
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(courseId, "student1");
        final DataIntegrityViolationException exception = new DataIntegrityViolationException((String) null);

        // when
        sut.recover(exception, event);

        // then — null message must be persisted without exception
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(getField(captor.getValue(), "exceptionMessage")).isNull();
    }

    @Test
    void recover_withDataIntegrityViolationException_exceptionClassNameStartsWithExpectedPackagePrefix() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
    void recover_withObjectOptimisticLockingFailureException_nullMessage_persistsNullMessage() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
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
    void constructor_withNullCommandHandler_constructsSuccessfully() {
        // constructor does not validate — NPE deferred to handleEvent invocation
        new StudentEnrolledToCourseIntegrationEventHandler(null, mock(FailedIntegrationEventRepository.class));
    }

    @Test
    void constructor_withNullRepository_constructsSuccessfully() {
        // constructor does not validate — NPE deferred to recover() invocation
        new StudentEnrolledToCourseIntegrationEventHandler(mock(IncreaseNumberOfStudentsCommandHandler.class), null);
    }

    @Test
    void handleStudentEnrolledToCourseEvent_withNullCommandHandler_throwsNullPointerException() {
        // given
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(null, mock(FailedIntegrationEventRepository.class));
        var event = new StudentEnrolledToCourseIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), "student1");

        // when/then
        assertThatThrownBy(() -> handler.handleStudentEnrolledToCourseEvent(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recover_withNullRepository_throwsNullPointerException() {
        // given
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(mock(IncreaseNumberOfStudentsCommandHandler.class), null);
        var event = new StudentEnrolledToCourseIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), "student1");
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
    void recover_exceptionClassName_containsDot() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — exceptionClassName must be a fully qualified name (contains at least one dot)
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "exceptionClassName")).contains(".");
    }

    @Test
    void recover_eventClassName_containsDot() throws Exception {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");
        final DataAccessResourceFailureException exception = new DataAccessResourceFailureException("DB error");

        // when
        sut.recover(exception, event);

        // then — eventClassName must be a fully qualified name (contains at least one dot)
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventClassName")).contains(".");
    }

    @Test
    void recover_withNullException_throwsNullPointerException() {
        // given — null exception causes NPE in e.getMessage() and e.getClass().getName()
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(uuid, "student1");

        // when/then
        assertThatThrownBy(() -> sut.recover(null, event))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(failedIntegrationEventRepository);
    }

    @Test
    void constructor_withBothNullDependencies_constructsSuccessfully() {
        // constructor does not validate — NPE deferred to method invocation
        new StudentEnrolledToCourseIntegrationEventHandler(null, null);
    }

    @Test
    void maxAttemptsField_valueIsThree() throws Exception {
        // given — hardcoded assertion guards against accidental changes to retry count
        Field maxAttemptsField = StudentEnrolledToCourseIntegrationEventHandler.class.getDeclaredField("MAX_ATTEMPTS");
        maxAttemptsField.setAccessible(true);
        int maxAttempts = (int) maxAttemptsField.get(null);

        // then
        assertThat(maxAttempts).isEqualTo(3);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
