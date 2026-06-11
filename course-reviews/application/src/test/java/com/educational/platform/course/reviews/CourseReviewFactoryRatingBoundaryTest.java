package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.course.ReviewableCourseRepository;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests boundary conditions for the rating field: negative values (@PositiveOrZero)
 * and values above the maximum (@Max(5)).
 */
@ExtendWith(MockitoExtension.class)
public class CourseReviewFactoryRatingBoundaryTest {

    @Mock
    private ReviewableCourseRepository reviewableCourseRepository;

    @Mock
    private CurrentUserAsReviewer currentUserAsReviewer;

    private CourseReviewFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseReviewFactory(validator, currentUserAsReviewer, reviewableCourseRepository);
    }

    @Test
    void createFrom_negativeRating_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID.randomUUID(), -1.0, "bad");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_ratingAboveMax_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID.randomUUID(), 5.1, "too high");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }
}
