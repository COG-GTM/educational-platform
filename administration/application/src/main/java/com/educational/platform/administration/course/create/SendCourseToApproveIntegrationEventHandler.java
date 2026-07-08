package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link SendCourseToApproveIntegrationEvent}, executes the logic for creating course proposal by {@link CreateCourseProposalCommandHandler}.
 * Executed asynchronously after the publishing transaction commits, with retry on failure.
 */
@Component
public class SendCourseToApproveIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SendCourseToApproveIntegrationEventHandler.class);

    private final CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    public SendCourseToApproveIntegrationEventHandler(CreateCourseProposalCommandHandler createCourseProposalCommandHandler) {
        this.createCourseProposalCommandHandler = createCourseProposalCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleSendCourseToApproveEvent(SendCourseToApproveIntegrationEvent event) {
        createCourseProposalCommandHandler.handle(new CreateCourseProposalCommand(event.courseId()));
    }

    @Recover
    void recover(Exception exception, SendCourseToApproveIntegrationEvent event) {
        log.error("Failed to process {} after retries, event will be dropped: {}",
                event.getClass().getSimpleName(), event, exception);
    }

}
