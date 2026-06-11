package com.educational.platform.course.reviews.edit;

import com.educational.platform.course.reviews.CourseReview;
import com.educational.platform.course.reviews.CourseReviewRepository;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@link UpdateCourseReviewCommandHandler} validates the
 * command AFTER finding the review (the validation happens post-lookup).
 */
@ExtendWith(MockitoExtension.class)
public class UpdateCourseReviewCommandHandlerValidationTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    private UpdateCourseReviewCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new UpdateCourseReviewCommandHandler(validator, courseReviewRepository);
    }

    @Test
    void handle_invalidRating_constraintViolationException() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseReview review = mock(CourseReview.class);
        when(courseReviewRepository.findByUuid(uuid)).thenReturn(Optional.of(review));

        // rating of -1 should violate constraints (if validated by @Min)
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, -1.0, "valid comment");

        // when
        final ThrowableAssert.ThrowingCallable action = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(action);
    }
}
