package com.educational.platform.course.reviews.create;

import com.educational.platform.course.reviews.CourseReviewFactory;
import com.educational.platform.course.reviews.CourseReviewRepository;
import com.educational.platform.course.reviews.CurrentUserAsReviewer;
import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReviewCourseCommandHandlerTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Test
    void handle_ratingAboveMaximum_rejectsBeforeLookupOrSave() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var factory = new CourseReviewFactory(validatorFactory.getValidator(), currentUserAsReviewer,
                    reviewableCourseRepository);
            var handler = new ReviewCourseCommandHandler(courseReviewRepository, factory);
            var command = new ReviewCourseCommand(UUID.randomUUID(), 6.0, "comment");

            assertThrows(ConstraintViolationException.class, () -> handler.handle(command));

            verifyNoInteractions(reviewableCourseRepository, currentUserAsReviewer, courseReviewRepository);
        }
    }
}
