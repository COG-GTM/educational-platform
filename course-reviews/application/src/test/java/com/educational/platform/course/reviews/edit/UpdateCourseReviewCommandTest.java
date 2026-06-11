package com.educational.platform.course.reviews.edit;

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

public class UpdateCourseReviewCommandTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void valid_noViolations() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 4.0, "updated");

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullUuid_violation() {
        // given
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(null, 4.0, "comment");

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void nullRating_violation() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, null, "comment");

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, -0.1, 6, 5.1})
    void invalidRating_violation(double rating) {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, rating, "comment");

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 2.5, 5.0})
    void validRatingBoundaries_noViolations(double rating) {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, rating, "comment");

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullComment_noViolations() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 4.0, null);

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void accessors_returnProvidedValues() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "updated");

        // then
        assertThat(command.uuid()).isEqualTo(uuid);
        assertThat(command.rating()).isEqualTo(3.0);
        assertThat(command.comment()).isEqualTo("updated");
    }

    @Test
    void emptyComment_noViolations() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 4.0, "");

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void equals_sameValues_returnsTrue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand first = new UpdateCourseReviewCommand(uuid, 3.0, "ok");
        final UpdateCourseReviewCommand second = new UpdateCourseReviewCommand(uuid, 3.0, "ok");

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentValues_returnsFalse() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand first = new UpdateCourseReviewCommand(uuid, 3.0, "ok");
        final UpdateCourseReviewCommand second = new UpdateCourseReviewCommand(uuid, 4.0, "great");

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void multipleNullFields_multipleViolations() {
        // given
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(null, null, "comment");

        // when
        final Set<ConstraintViolation<UpdateCourseReviewCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }
}
