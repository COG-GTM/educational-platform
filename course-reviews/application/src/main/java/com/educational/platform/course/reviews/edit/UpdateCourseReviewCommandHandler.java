package com.educational.platform.course.reviews.edit;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.rating.CourseRatingRecalculator;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;

/**
 * Command handler for {@link UpdateCourseReviewCommand} updates a course review.
 */
@Component
@Transactional
public class UpdateCourseReviewCommandHandler {

    private final Validator validator;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseRatingRecalculator courseRatingRecalculator;

    public UpdateCourseReviewCommandHandler(Validator validator, CourseReviewRepository courseReviewRepository,
                                           CourseRatingRecalculator courseRatingRecalculator) {
        this.validator = validator;
        this.courseReviewRepository = courseReviewRepository;
        this.courseRatingRecalculator = courseRatingRecalculator;
    }

    /**
     * Updates course review by values from command.
     *
     * @param command command
     * @throws ResourceNotFoundException    course review not found
     * @throws ConstraintViolationException validation issues
     */
    @PreAuthorize("hasRole('STUDENT') and @courseReviewChecker.hasAccess(authentication, #c.uuid)")
    public void handle(@P("c") UpdateCourseReviewCommand command) {
        final Optional<CourseReview> dbResult = courseReviewRepository.findByUuid(command.uuid());
        if (dbResult.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Course Review with uuid: %s not found", command.uuid()));
        }

        // todo move to validator
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        final CourseReview review = dbResult.get();
        review.update(command);
        courseReviewRepository.save(review);
        courseRatingRecalculator.recalculateForReview(command.uuid());
    }

}
