package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests Jakarta Validation constraints on {@link CreateQuestionCommand}.
 * The {@code content} field is annotated with {@code @NotBlank}.
 */
public class CreateQuestionCommandValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validContent_noViolations() {
        // given
        final CreateQuestionCommand command = new CreateQuestionCommand("What is Java?");

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullContent_hasViolation() {
        // given
        final CreateQuestionCommand command = new CreateQuestionCommand(null);

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void emptyContent_hasViolation() {
        // given
        final CreateQuestionCommand command = new CreateQuestionCommand("");

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void blankContent_hasViolation() {
        // given
        final CreateQuestionCommand command = new CreateQuestionCommand("   ");

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void singleCharacterContent_noViolations() {
        // given
        final CreateQuestionCommand command = new CreateQuestionCommand("Q");

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(command);

        // then
        assertThat(violations).isEmpty();
    }
}
