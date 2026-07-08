package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link SendCourseToApproveIntegrationEvent}, executes the logic for creating course proposal by {@link CreateCourseProposalCommandHandler}.
 */
@Component
public class SendCourseToApproveIntegrationEventHandler {

    private final SendCourseToApproveRetryableInvoker sendCourseToApproveRetryableInvoker;

    public SendCourseToApproveIntegrationEventHandler(SendCourseToApproveRetryableInvoker sendCourseToApproveRetryableInvoker) {
        this.sendCourseToApproveRetryableInvoker = sendCourseToApproveRetryableInvoker;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSendCourseToApproveEvent(SendCourseToApproveIntegrationEvent event) {
        sendCourseToApproveRetryableInvoker.invoke(event);
    }

}
