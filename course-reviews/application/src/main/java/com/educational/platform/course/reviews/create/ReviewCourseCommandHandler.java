package com.educational.platform.course.reviews.create;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;

import jakarta.annotation.Nonnull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
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

    public ReviewCourseCommandHandler(CourseReviewRepository courseReviewRepository, CourseReviewFactory courseReviewFactory) {
        this.courseReviewRepository = courseReviewRepository;
        this.courseReviewFactory = courseReviewFactory;
    }

    /**
     * Creates course review from command. Only a student enrolled to the course is allowed to review it.
     *
     * @param command command
     * @return uuid
     * @throws ConstraintViolationException          in the case of validation issues
     * @throws RelatedResourceIsNotResolvedException if course or reviewer is not found by relation
     * @throws UnprocessableEntityException          if the course is already reviewed by the current user
     */
    @Nonnull
    @PreAuthorize("hasRole('STUDENT') and @courseReviewChecker.isEnrolled(authentication, #c.courseId)")
    public UUID handle(@P("c") ReviewCourseCommand command) {
        final CourseReview courseReview = courseReviewFactory.createFrom(command);
        courseReviewRepository.save(courseReview);

        return courseReview.toIdentifier();
    }

}
