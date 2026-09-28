package com.educational.platform.course.reviews;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * Represents Course Review Factory.
 */
@Component
public class CourseReviewFactory {

    private final Validator validator;
    private final CurrentUserAsReviewer currentUserAsReviewer;
    private final ReviewableCourseRepository reviewableCourseRepository;
    private final CourseReviewRepository courseReviewRepository;

    public CourseReviewFactory(Validator validator, CurrentUserAsReviewer currentUserAsReviewer, ReviewableCourseRepository reviewableCourseRepository, CourseReviewRepository courseReviewRepository) {
        this.validator = validator;
        this.currentUserAsReviewer = currentUserAsReviewer;
        this.reviewableCourseRepository = reviewableCourseRepository;
        this.courseReviewRepository = courseReviewRepository;
    }

    /**
     * Creates course review from command.
     *
     * @param reviewCourseCommand review course command
     * @return course review
     * @throws ConstraintViolationException          in the case of validation issues
     * @throws RelatedResourceIsNotResolvedException if course or reviewer is not found by relation
     * @throws UnprocessableEntityException          if the course is already reviewed by the current user
     */
    public CourseReview createFrom(ReviewCourseCommand reviewCourseCommand) {
        final Set<ConstraintViolation<ReviewCourseCommand>> violations = validator.validate(reviewCourseCommand);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        var course = reviewableCourseRepository.findByOriginalCourseId(reviewCourseCommand.courseId())
                .orElseThrow(() -> new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + reviewCourseCommand.courseId()));

        var reviewer = currentUserAsReviewer.userAsReviewer();

        if (courseReviewRepository.existsByCourseAndReviewer(course.getId(), reviewer.getId())) {
            throw new UnprocessableEntityException("Course with uuid = " + reviewCourseCommand.courseId() + " is already reviewed by the current user");
        }

        return new CourseReview(reviewCourseCommand, course.getId(), reviewer.getId());
    }

}
