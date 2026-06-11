package com.educational.platform.course.reviews.create;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewCourseCommandTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void valid_noViolations() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "great course");

        // when
        final Set<ConstraintViolation<ReviewCourseCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullCourseId_violation() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(null, 4.0, "comment");

        // when
        final Set<ConstraintViolation<ReviewCourseCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void nullRating_violation() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, null, "comment");

        // when
        final Set<ConstraintViolation<ReviewCourseCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, -0.1, 6, 5.1})
    void invalidRating_violation(double rating) {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, rating, "comment");

        // when
        final Set<ConstraintViolation<ReviewCourseCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 2.5, 5.0})
    void validRatingBoundaries_noViolations(double rating) {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, rating, "comment");

        // when
        final Set<ConstraintViolation<ReviewCourseCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullComment_noViolations() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, null);

        // when
        final Set<ConstraintViolation<ReviewCourseCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void accessors_returnProvidedValues() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        // then
        assertThat(command.courseId()).isEqualTo(courseId);
        assertThat(command.rating()).isEqualTo(4.0);
        assertThat(command.comment()).isEqualTo("comment");
    }
}
