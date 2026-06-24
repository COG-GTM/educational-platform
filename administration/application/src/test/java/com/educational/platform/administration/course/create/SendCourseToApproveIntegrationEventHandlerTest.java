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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SendCourseToApproveIntegrationEventHandlerTest {

    @Mock
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedEventRepository;

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
    void handleSendCourseToApproveEvent_transientException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void handleSendCourseToApproveEvent_businessException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleSendCourseToApproveEvent_pessimisticLockingException_rethrown() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        doThrow(new PessimisticLockingFailureException("pessimistic lock"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleSendCourseToApproveEvent(event))
                .isInstanceOf(PessimisticLockingFailureException.class);
    }

    @Test
    void handleSendCourseToApproveEvent_successfulHandling_doesNotPersistFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);

        // when
        sut.handleSendCourseToApproveEvent(event);

        // then
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_persistsFailedEvent() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("DB connection lost");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
        assertThat(failedEvent.getEventPayload()).isEqualTo(event.toString());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("DB connection lost");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void recover_withNullExceptionMessage_persistsWithNullMessage() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(uuid);
        final Exception exception = new RuntimeException((String) null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage()).isNull();
    }

}
