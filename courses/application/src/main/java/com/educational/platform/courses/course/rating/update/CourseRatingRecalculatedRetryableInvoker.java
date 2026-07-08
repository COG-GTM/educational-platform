package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.retry.IntegrationEventRetryPolicy;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class CourseRatingRecalculatedRetryableInvoker {

    private static final Logger log = LoggerFactory.getLogger(CourseRatingRecalculatedRetryableInvoker.class);

    private final UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    public CourseRatingRecalculatedRetryableInvoker(UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler) {
        this.updateCourseRatingCommandHandler = updateCourseRatingCommandHandler;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = IntegrationEventRetryPolicy.MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = IntegrationEventRetryPolicy.INITIAL_DELAY_MS,
                    multiplier = IntegrationEventRetryPolicy.MULTIPLIER,
                    maxDelay = IntegrationEventRetryPolicy.MAX_DELAY_MS))
    public void invoke(CourseRatingRecalculatedIntegrationEvent event) {
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(event.courseId(), event.rating()));
    }

    @Recover
    public void recover(Exception ex, CourseRatingRecalculatedIntegrationEvent event) {
        log.error("Integration event '{}' exhausted retries after {} attempts; payload={}",
                event.getClass().getSimpleName(), IntegrationEventRetryPolicy.MAX_ATTEMPTS, event, ex);
    }
}
