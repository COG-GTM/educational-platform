package com.educational.platform.web.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link GlobalExceptionHandler#onConstraintViolationException} with actual constraint violations.
 */
public class GlobalExceptionHandlerConstraintViolationsTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    record SampleInput(@NotBlank String name, @NotBlank String description) {
    }

    @Test
    void onConstraintViolationException_singleViolation_returnsViolationMessage() {
        // given
        final Set<ConstraintViolation<SampleInput>> violations = validator.validate(new SampleInput("valid", ""));

        // when
        final ResponseEntity<ErrorResponse> response =
                sut.onConstraintViolationException(new ConstraintViolationException(violations));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().getFirst()).isNotBlank();
    }

    @Test
    void onConstraintViolationException_multipleViolations_returnsAllMessages() {
        // given
        final Set<ConstraintViolation<SampleInput>> violations = validator.validate(new SampleInput("", ""));

        // when
        final ResponseEntity<ErrorResponse> response =
                sut.onConstraintViolationException(new ConstraintViolationException(violations));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
    }

    @Test
    void onConstraintViolationException_validInput_emptyErrors() {
        // given — no violations from valid input
        final Set<ConstraintViolation<SampleInput>> violations = validator.validate(new SampleInput("name", "desc"));

        // when
        final ResponseEntity<ErrorResponse> response =
                sut.onConstraintViolationException(new ConstraintViolationException(violations));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).isEmpty();
    }
}
