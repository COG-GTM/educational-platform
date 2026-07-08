package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link SendCourseToApproveIntegrationEvent}, executes the logic for creating course proposal by {@link CreateCourseProposalCommandHandler}.
 */
@Component
public class SendCourseToApproveIntegrationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(SendCourseToApproveIntegrationEventHandler.class);

    private final CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    public SendCourseToApproveIntegrationEventHandler(CreateCourseProposalCommandHandler createCourseProposalCommandHandler) {
        this.createCourseProposalCommandHandler = createCourseProposalCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleSendCourseToApproveEvent(SendCourseToApproveIntegrationEvent event) {
        createCourseProposalCommandHandler.handle(new CreateCourseProposalCommand(event.courseId()));
    }

    @Recover
    public void recover(Throwable e, SendCourseToApproveIntegrationEvent event) {
        logger.error("Integration event ultimately failed after retries: {}", event, e);
    }

}
