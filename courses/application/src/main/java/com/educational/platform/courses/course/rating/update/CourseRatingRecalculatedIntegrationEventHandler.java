package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

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
 * Event listener for {@link CourseRatingRecalculatedIntegrationEvent}.
 */
@Component
public class CourseRatingRecalculatedIntegrationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CourseRatingRecalculatedIntegrationEventHandler.class);

    private final UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    public CourseRatingRecalculatedIntegrationEventHandler(UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler) {
        this.updateCourseRatingCommandHandler = updateCourseRatingCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleCourseRatingRecalculatedEvent(CourseRatingRecalculatedIntegrationEvent event) {
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(event.courseId(), event.rating()));
    }

    @Recover
    public void recover(Throwable e, CourseRatingRecalculatedIntegrationEvent event) {
        logger.error("Integration event ultimately failed after retries: {}", event, e);
    }

}
