package com.educational.platform.administration.course.create;

import com.educational.platform.common.retry.IntegrationEventRetryPolicy;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class SendCourseToApproveRetryableInvoker {

    private static final Logger log = LoggerFactory.getLogger(SendCourseToApproveRetryableInvoker.class);

    private final CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    public SendCourseToApproveRetryableInvoker(CreateCourseProposalCommandHandler createCourseProposalCommandHandler) {
        this.createCourseProposalCommandHandler = createCourseProposalCommandHandler;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = IntegrationEventRetryPolicy.MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = IntegrationEventRetryPolicy.INITIAL_DELAY_MS,
                    multiplier = IntegrationEventRetryPolicy.MULTIPLIER,
                    maxDelay = IntegrationEventRetryPolicy.MAX_DELAY_MS))
    public void invoke(SendCourseToApproveIntegrationEvent event) {
        createCourseProposalCommandHandler.handle(new CreateCourseProposalCommand(event.courseId()));
    }

    @Recover
    public void recover(Exception ex, SendCourseToApproveIntegrationEvent event) {
        log.error("Integration event '{}' exhausted retries after {} attempts; payload={}",
                event.getClass().getSimpleName(), IntegrationEventRetryPolicy.MAX_ATTEMPTS, event, ex);
    }
}
