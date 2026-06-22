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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;

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
        final Exception exception = new DataAccessResourceFailureException("DB connection lost");

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

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
