package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventEntity;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.FailedIntegrationEventStatus;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedIntegrationEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand createTeacherCommand = argument.getValue();
        assertThat(createTeacherCommand)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void recover_persistsFailedIntegrationEventWithSerializedPayloadAndExceptionMessage() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.recover(new RuntimeException("boom"), event);

        // then
        final ArgumentCaptor<FailedIntegrationEventEntity> captor = ArgumentCaptor.forClass(FailedIntegrationEventEntity.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final FailedIntegrationEventEntity saved = captor.getValue();
        assertThat(saved.getEventClassName()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
        assertThat(saved.getExceptionMessage()).isEqualTo("boom");
        assertThat(saved.getRetryCount()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(FailedIntegrationEventStatus.FAILED);
        assertThat(saved.getEventPayload()).contains("username");
    }

    @Test
    void recover_whenPayloadSerializationFails_fallsBackToStringValueOfEvent() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");
        doThrow(new RuntimeException("serialization failure")).when(objectMapper).writeValueAsString(any());

        // when
        sut.recover(new RuntimeException("boom"), event);

        // then
        final ArgumentCaptor<FailedIntegrationEventEntity> captor = ArgumentCaptor.forClass(FailedIntegrationEventEntity.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventPayload()).isEqualTo(String.valueOf(event));
    }

}
