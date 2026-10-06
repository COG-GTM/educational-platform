package com.educational.platform.course.reviews.rating;

import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.course.reviews.query.CourseReviewSummaryDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Recalculates the average rating of a course from its reviews and publishes
 * {@link CourseRatingRecalculatedIntegrationEvent} so other modules (e.g. courses) can pick it up.
 */
@Component
public class CourseRatingRecalculator {

    private final CourseReviewRepository courseReviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CourseRatingRecalculator(CourseReviewRepository courseReviewRepository, ApplicationEventPublisher eventPublisher) {
        this.courseReviewRepository = courseReviewRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Recalculates the rating of the given course and publishes the result.
     *
     * @param courseUuid course uuid
     */
    public void recalculate(UUID courseUuid) {
        final double average = CourseReviewSummaryDTO.from(courseReviewRepository.listRatings(courseUuid)).averageRating();
        eventPublisher.publishEvent(new CourseRatingRecalculatedIntegrationEvent(courseUuid, average));
    }

    /**
     * Recalculates the rating of the course the given review belongs to.
     *
     * @param reviewUuid course review uuid
     */
    public void recalculateForReview(UUID reviewUuid) {
        courseReviewRepository.findCourseUuid(reviewUuid).ifPresent(this::recalculate);
    }
}
