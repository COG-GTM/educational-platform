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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class CourseReviewFactoryNullInputTest {

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
    void createFrom_nullCourseId_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(null, 4.0, "comment");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_nullRating_constraintViolationException() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(java.util.UUID.randomUUID(), null, "comment");

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_zeroRating_noValidationError() {
        // given — boundary: 0 is the minimum valid rating (@PositiveOrZero)
        final java.util.UUID courseId = java.util.UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 0.0, "comment");

        final var reviewableCourse = new com.educational.platform.course.reviews.course.ReviewableCourse(
                new com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand(courseId));
        org.springframework.test.util.ReflectionTestUtils.setField(reviewableCourse, "id", 10);
        org.mockito.Mockito.when(reviewableCourseRepository.findByOriginalCourseId(courseId))
                .thenReturn(java.util.Optional.of(reviewableCourse));

        final var reviewer = new com.educational.platform.course.reviews.reviewer.Reviewer(
                new com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand("user"));
        org.springframework.test.util.ReflectionTestUtils.setField(reviewer, "id", 20);
        org.mockito.Mockito.when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        org.assertj.core.api.Assertions.assertThat(result)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void createFrom_maxRating_noValidationError() {
        // given — boundary: 5 is the maximum valid rating (@Max(5))
        final java.util.UUID courseId = java.util.UUID.randomUUID();
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.0, "comment");

        final var reviewableCourse = new com.educational.platform.course.reviews.course.ReviewableCourse(
                new com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand(courseId));
        org.springframework.test.util.ReflectionTestUtils.setField(reviewableCourse, "id", 10);
        org.mockito.Mockito.when(reviewableCourseRepository.findByOriginalCourseId(courseId))
                .thenReturn(java.util.Optional.of(reviewableCourse));

        final var reviewer = new com.educational.platform.course.reviews.reviewer.Reviewer(
                new com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand("user"));
        org.springframework.test.util.ReflectionTestUtils.setField(reviewer, "id", 20);
        org.mockito.Mockito.when(currentUserAsReviewer.userAsReviewer()).thenReturn(reviewer);

        // when
        final CourseReview result = sut.createFrom(command);

        // then
        org.assertj.core.api.Assertions.assertThat(result)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }
}
