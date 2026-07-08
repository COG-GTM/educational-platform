package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SendCourseToApproveIntegrationEventHandlerTest {

    @Mock
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    @InjectMocks
    private SendCourseToApproveIntegrationEventHandler sut;

    @Test
    void handleCourseApprovedByAdminEvent_approveCourseCommandExecuted() {
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
    void handleCourseApprovedByAdminEvent_isAnnotatedForAfterCommitAsyncRetryAndRecover() throws NoSuchMethodException {
        final Method method = SendCourseToApproveIntegrationEventHandler.class.getDeclaredMethod(
                "handleSendCourseToApproveEvent",
                SendCourseToApproveIntegrationEvent.class);

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

        final boolean recoverMethodExists = java.util.Arrays.stream(SendCourseToApproveIntegrationEventHandler.class.getDeclaredMethods())
                .anyMatch(candidate -> candidate.isAnnotationPresent(org.springframework.retry.annotation.Recover.class));
        assertThat(recoverMethodExists).isTrue();
    }

}
