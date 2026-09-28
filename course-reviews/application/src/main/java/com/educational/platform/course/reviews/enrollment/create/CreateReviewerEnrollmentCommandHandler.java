package com.educational.platform.course.reviews.enrollment.create;

import com.educational.platform.course.reviews.enrollment.ReviewerEnrollment;
import com.educational.platform.course.reviews.enrollment.ReviewerEnrollmentRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * Command handler for {@link CreateReviewerEnrollmentCommand} creates a reviewer enrollment. Runs in its own transaction
 * because it is invoked from an after-commit listener of the publishing enrollment transaction.
 */
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CreateReviewerEnrollmentCommandHandler {

    private final Validator validator;
    private final ReviewerEnrollmentRepository reviewerEnrollmentRepository;

    public CreateReviewerEnrollmentCommandHandler(Validator validator, ReviewerEnrollmentRepository reviewerEnrollmentRepository) {
        this.validator = validator;
        this.reviewerEnrollmentRepository = reviewerEnrollmentRepository;
    }

    /**
     * Creates reviewer enrollment from command, already existing enrollment is left untouched.
     *
     * @param command command
     * @throws ConstraintViolationException in the case of validation issues
     */
    public void handle(CreateReviewerEnrollmentCommand command) {
        final Set<ConstraintViolation<CreateReviewerEnrollmentCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        if (reviewerEnrollmentRepository.existsByCourseIdAndUsername(command.courseId(), command.username())) {
            return;
        }

        reviewerEnrollmentRepository.save(new ReviewerEnrollment(command));
    }

}
