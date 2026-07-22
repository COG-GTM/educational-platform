package com.educational.platform.web.handler;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler sut;

    @BeforeEach
    void setUp() {
        sut = new GlobalExceptionHandler();
    }

    @Test
    void onMethodArgumentNotValidException_fieldErrors_badRequestWithMessages() throws NoSuchMethodException {
        // given
        final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "field", "must not be null"));
        final Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class);
        final MethodArgumentNotValidException e = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsExactly("must not be null");
    }

    @Test
    void onConstraintViolationException_violations_badRequestWithMessages() {
        // given
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final ConstraintViolationException e = new ConstraintViolationException(validator.validate(new Sample(null)));

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConstraintViolationException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsExactly("must not be null");
    }

    @Test
    void onRelatedResourceIsNotResolvedException_exception_badRequestWithMessage() {
        // given
        final RelatedResourceIsNotResolvedException e = new RelatedResourceIsNotResolvedException("related resource is not resolved");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onRelatedResourceIsNotResolvedException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsExactly("related resource is not resolved");
    }

    @Test
    void onResourceNotFoundException_exception_notFoundWithMessage() {
        // given
        final ResourceNotFoundException e = new ResourceNotFoundException("resource not found");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onResourceNotFoundException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errors()).containsExactly("resource not found");
    }

    @Test
    void onUnprocessableEntityException_exception_unprocessableEntityWithMessage() {
        // given
        final UnprocessableEntityException e = new UnprocessableEntityException("unprocessable entity");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onUnprocessableEntityException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().errors()).containsExactly("unprocessable entity");
    }

    @Test
    void onException_exception_internalServerErrorWithMessage() {
        // given
        final Exception e = new Exception("unexpected error");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errors()).containsExactly("unexpected error");
    }

    @Test
    void onException_exceptionWithoutMessage_internalServerErrorWithEmptyErrors() {
        // given
        final Exception e = new Exception();

        // when
        final ResponseEntity<ErrorResponse> response = sut.onException(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errors()).isEmpty();
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String argument) {
    }

    private record Sample(@NotNull String name) {
    }
}
