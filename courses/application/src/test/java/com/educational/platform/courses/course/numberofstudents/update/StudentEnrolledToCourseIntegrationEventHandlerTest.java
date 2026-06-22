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

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
