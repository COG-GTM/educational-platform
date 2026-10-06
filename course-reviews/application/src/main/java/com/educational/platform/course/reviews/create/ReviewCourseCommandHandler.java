package com.educational.platform.course.reviews.create;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.rating.CourseRatingRecalculator;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolationException;
import java.util.UUID;

/**
 * Command handler for {@link ReviewCourseCommand} creates a course review.
 */
@Component
@Transactional
public class ReviewCourseCommandHandler {

    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewFactory courseReviewFactory;
    private final CourseRatingRecalculator courseRatingRecalculator;

    public ReviewCourseCommandHandler(CourseReviewRepository courseReviewRepository, CourseReviewFactory courseReviewFactory,
                                     CourseRatingRecalculator courseRatingRecalculator) {
        this.courseReviewRepository = courseReviewRepository;
        this.courseReviewFactory = courseReviewFactory;
        this.courseRatingRecalculator = courseRatingRecalculator;
    }

    /**
     * Creates course review from command.
     *
     * @param command command
     * @return uuid
     * @throws ConstraintViolationException          in the case of validation issues
     * @throws RelatedResourceIsNotResolvedException if course or reviewer is not found by relation
     */
    @Nonnull
    public UUID handle(ReviewCourseCommand command) {
        final CourseReview courseReview = courseReviewFactory.createFrom(command);
        courseReviewRepository.save(courseReview);
        courseRatingRecalculator.recalculate(command.courseId());

        return courseReview.toIdentifier();
    }

}
