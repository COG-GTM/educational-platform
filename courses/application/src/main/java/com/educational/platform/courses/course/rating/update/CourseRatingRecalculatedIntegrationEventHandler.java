package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link CourseRatingRecalculatedIntegrationEvent}.
 */
@Component
public class CourseRatingRecalculatedIntegrationEventHandler {

    private final CourseRatingRecalculatedRetryableInvoker courseRatingRecalculatedRetryableInvoker;

    public CourseRatingRecalculatedIntegrationEventHandler(CourseRatingRecalculatedRetryableInvoker courseRatingRecalculatedRetryableInvoker) {
        this.courseRatingRecalculatedRetryableInvoker = courseRatingRecalculatedRetryableInvoker;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseRatingRecalculatedEvent(CourseRatingRecalculatedIntegrationEvent event) {
        courseRatingRecalculatedRetryableInvoker.invoke(event);
    }

}
