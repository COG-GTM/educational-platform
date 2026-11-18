package com.educational.platform.courses.course.rating.update;

import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link CourseRatingRecalculatedIntegrationEvent}.
 */
@Component
@ConditionalOnProperty(name = "courses.in-process-handlers.enabled", havingValue = "true", matchIfMissing = true)
public class CourseRatingRecalculatedIntegrationEventHandler {

    private final UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    public CourseRatingRecalculatedIntegrationEventHandler(UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler) {
        this.updateCourseRatingCommandHandler = updateCourseRatingCommandHandler;
    }

    @Async
    @EventListener
    public void handleCourseRatingRecalculatedEvent(CourseRatingRecalculatedIntegrationEvent event) {
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(event.courseId(), event.rating()));
    }

}
