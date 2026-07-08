package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");

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
    void handleUserCreatedEvent_isAnnotatedForAfterCommitAsyncRetryAndRecover() throws NoSuchMethodException {
        final Method method = UserCreatedIntegrationEventHandler.class.getDeclaredMethod(
                "handleUserCreatedEvent",
                UserCreatedIntegrationEvent.class);

        assertThat(method.isAnnotationPresent(TransactionalEventListener.class)).isTrue();
        final TransactionalEventListener transactionalEventListener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(transactionalEventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);

        assertThat(method.isAnnotationPresent(Async.class)).isTrue();
        final Async async = method.getAnnotation(Async.class);
        assertThat(async.value()).isEqualTo("integrationEventExecutor");

        assertThat(method.isAnnotationPresent(Retryable.class)).isTrue();
        final Retryable retryable = method.getAnnotation(Retryable.class);
        assertThat(retryable.maxAttempts()).isEqualTo(3);
        assertThat(retryable.backoff().delay()).isEqualTo(500L);
        assertThat(retryable.backoff().multiplier()).isEqualTo(2.0d);

        final boolean recoverMethodExists = java.util.Arrays.stream(UserCreatedIntegrationEventHandler.class.getDeclaredMethods())
                .anyMatch(candidate -> candidate.isAnnotationPresent(org.springframework.retry.annotation.Recover.class));
        assertThat(recoverMethodExists).isTrue();
    }
}
