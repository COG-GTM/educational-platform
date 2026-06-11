package com.educational.platform.courses.course.create;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean-validation tests for {@link CreateQuestionCommand} ensuring the
 * {@code @NotBlank} constraint on {@code content} is enforced by the
 * validator.
 */
public class CreateQuestionCommandValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validContent_noViolations() {
        final CreateQuestionCommand command = new CreateQuestionCommand("What is Java?");
        assertThat(validator.validate(command)).isEmpty();
    }

    @Test
    void nullContent_hasViolation() {
        final CreateQuestionCommand command = new CreateQuestionCommand(null);
        assertThat(validator.validate(command)).isNotEmpty();
    }

    @Test
    void emptyContent_hasViolation() {
        final CreateQuestionCommand command = new CreateQuestionCommand("");
        assertThat(validator.validate(command)).isNotEmpty();
    }

    @Test
    void blankContent_hasViolation() {
        final CreateQuestionCommand command = new CreateQuestionCommand("   ");
        assertThat(validator.validate(command)).isNotEmpty();
    }

    @Test
    void singleCharContent_noViolations() {
        final CreateQuestionCommand command = new CreateQuestionCommand("Q");
        assertThat(validator.validate(command)).isEmpty();
    }
}
