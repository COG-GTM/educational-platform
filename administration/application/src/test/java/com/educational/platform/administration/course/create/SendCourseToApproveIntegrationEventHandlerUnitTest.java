package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SendCourseToApproveIntegrationEventHandlerUnitTest {

    @Mock
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    private SendCourseToApproveIntegrationEventHandler sut;

    @BeforeEach
    void setUp() {
        sut = new SendCourseToApproveIntegrationEventHandler(createCourseProposalCommandHandler);
    }

    @Test
    void handleSendCourseToApproveEvent_delegatesToCommandHandler() {
        // given
        final UUID courseId = UUID.randomUUID();
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(courseId);

        // when
        sut.handleSendCourseToApproveEvent(event);

        // then
        final ArgumentCaptor<CreateCourseProposalCommand> captor = ArgumentCaptor.forClass(CreateCourseProposalCommand.class);
        verify(createCourseProposalCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().uuid()).isEqualTo(courseId);
    }
}
