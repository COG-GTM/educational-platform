package com.educational.platform.administration.course.create;

import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link SendCourseToApproveIntegrationEvent}, executes the logic for creating course proposal by {@link CreateCourseProposalCommandHandler}.
 */
@Component
public class SendCourseToApproveIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SendCourseToApproveIntegrationEventHandler.class);

    private final CreateCourseProposalCommandHandler createCourseProposalCommandHandler;
    private final FailedIntegrationEventRepository failedEventRepository;

    public SendCourseToApproveIntegrationEventHandler(CreateCourseProposalCommandHandler createCourseProposalCommandHandler,
                                                      FailedIntegrationEventRepository failedEventRepository) {
        this.createCourseProposalCommandHandler = createCourseProposalCommandHandler;
        this.failedEventRepository = failedEventRepository;
    }

    @Async
    @EventListener
    @Retryable(retryFor = { DataAccessException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleSendCourseToApproveEvent(SendCourseToApproveIntegrationEvent event) {
        log.info("Received SendCourseToApproveIntegrationEvent: {}", event);
        try {
            createCourseProposalCommandHandler.handle(new CreateCourseProposalCommand(event.courseId()));
        } catch (RuntimeException ex) {
            log.error("Failed to handle SendCourseToApproveIntegrationEvent: {}", event, ex);
            throw ex;
        }
    }

    @Recover
    public void recover(Throwable ex, SendCourseToApproveIntegrationEvent event) {
        log.error("All retries exhausted handling SendCourseToApproveIntegrationEvent: {}", event, ex);
        failedEventRepository.save(FailedIntegrationEvent.of(event.getClass().getName(), String.valueOf(event),
                ex.getMessage(), IntegrationEventRetryHandler.MAX_ATTEMPTS));
    }

}
