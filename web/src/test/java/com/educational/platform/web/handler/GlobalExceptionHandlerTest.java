package com.educational.platform.web.handler;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void onMethodArgumentNotValidException_collectsFieldDefaultMessagesAsBadRequest() {
        // given - bean validation failures on a request body are surfaced as a 400 listing every field's
        // default message
        final BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("course", "title", "must not be blank"),
                new FieldError("course", "price", "must be positive")));
        final MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors())
                .containsExactly("must not be blank", "must be positive");
    }

    @Test
    void onConstraintViolationException_collectsViolationMessagesAsBadRequest() {
        // given - method-level constraint violations are surfaced as a 400 listing every violation message
        final ConstraintViolation<?> first = mock(ConstraintViolation.class);
        final ConstraintViolation<?> second = mock(ConstraintViolation.class);
        when(first.getMessage()).thenReturn("size must be between 1 and 10");
        when(second.getMessage()).thenReturn("must not be null");
        final ConstraintViolationException exception =
                new ConstraintViolationException(Set.of(first, second));

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConstraintViolationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors())
                .containsExactlyInAnyOrder("size must be between 1 and 10", "must not be null");
    }

    @Test
    void onRelatedResourceIsNotResolvedException_returnsBadRequestWithSingleMessage() {
        // given
        final RelatedResourceIsNotResolvedException exception =
                new RelatedResourceIsNotResolvedException("course is not resolved");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onRelatedResourceIsNotResolvedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("course is not resolved");
    }

    @Test
    void onResourceNotFoundException_returnsNotFoundWithSingleMessage() {
        // given
        final ResourceNotFoundException exception = new ResourceNotFoundException("course not found");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onResourceNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("course not found");
    }

    @Test
    void onUnprocessableEntityException_returnsUnprocessableEntityWithSingleMessage() {
        // given
        final UnprocessableEntityException exception = new UnprocessableEntityException("invalid credentials");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onUnprocessableEntityException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("invalid credentials");
    }

    @Test
    void onException_returnsInternalServerErrorWithSingleMessage() {
        // given - any unmapped exception falls through to a 500
        final Exception exception = new Exception("unexpected failure");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("unexpected failure");
    }

    @Test
    void onResourceNotFoundException_nullMessage_returnsNotFoundWithEmptyErrors() {
        // given - a null exception message must not produce a [null] error list; ErrorResponse maps it to empty
        final ResourceNotFoundException exception = new ResourceNotFoundException(null);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onResourceNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).isEmpty();
    }
}
