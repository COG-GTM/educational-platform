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

@ExtendWith(MockitoExtension.class)
class CourseReviewFactoryNullRatingTest {

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
    void createFrom_nullRating_constraintViolationException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, null, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }

    @Test
    void createFrom_nullCourseId_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(null, 4.0, "comment");

        // when
        final org.assertj.core.api.ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }
}
