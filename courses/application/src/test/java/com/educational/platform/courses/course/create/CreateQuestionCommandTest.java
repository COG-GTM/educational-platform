package com.educational.platform.courses.course.create;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateQuestionCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void constructor_contentStored() {
        // when
        final CreateQuestionCommand sut = new CreateQuestionCommand("What is Java?");

        // then
        assertThat(sut.content()).isEqualTo("What is Java?");
    }

    @Test
    void constructor_nullContent_storedAsNull() {
        // when
        final CreateQuestionCommand sut = new CreateQuestionCommand(null);

        // then
        assertThat(sut.content()).isNull();
    }

    @Test
    void constructor_emptyContent_storedAsEmpty() {
        // when
        final CreateQuestionCommand sut = new CreateQuestionCommand("");

        // then
        assertThat(sut.content()).isEmpty();
    }

    @Test
    void validation_blankContent_violationReported() {
        // given
        final CreateQuestionCommand sut = new CreateQuestionCommand("");

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(sut);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
    }

    @Test
    void validation_nullContent_violationReported() {
        // given
        final CreateQuestionCommand sut = new CreateQuestionCommand(null);

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(sut);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validation_validContent_noViolations() {
        // given
        final CreateQuestionCommand sut = new CreateQuestionCommand("Valid question");

        // when
        final Set<ConstraintViolation<CreateQuestionCommand>> violations = validator.validate(sut);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void equalInstances_areEqual() {
        // given
        final CreateQuestionCommand a = new CreateQuestionCommand("Q1");
        final CreateQuestionCommand b = new CreateQuestionCommand("Q1");

        // then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentInstances_areNotEqual() {
        // given
        final CreateQuestionCommand a = new CreateQuestionCommand("Q1");
        final CreateQuestionCommand b = new CreateQuestionCommand("Q2");

        // then
        assertThat(a).isNotEqualTo(b);
    }
}
