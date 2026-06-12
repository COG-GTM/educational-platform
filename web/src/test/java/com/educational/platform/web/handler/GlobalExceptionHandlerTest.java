package com.educational.platform.web.handler;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler sut;

    @BeforeEach
    void setUp() {
        sut = new GlobalExceptionHandler();
    }

    @Test
    void onResourceNotFoundException_returnsNotFound() {
        // given
        final ResourceNotFoundException exception = new ResourceNotFoundException("Course not found");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onResourceNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Course not found");
    }

    @Test
    void onUnprocessableEntityException_returnsUnprocessableEntity() {
        // given
        final UnprocessableEntityException exception = new UnprocessableEntityException("Invalid data");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onUnprocessableEntityException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Invalid data");
    }

    @Test
    void onRelatedResourceIsNotResolvedException_returnsBadRequest() {
        // given
        final RelatedResourceIsNotResolvedException exception = new RelatedResourceIsNotResolvedException("Course cannot be found");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onRelatedResourceIsNotResolvedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Course cannot be found");
    }

    @SuppressWarnings("unchecked")
    @Test
    void onConstraintViolationException_returnsBadRequest() {
        // given
        final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be null");
        final ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConstraintViolationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("must not be null");
    }

    @Test
    void onException_returnsInternalServerError() {
        // given
        final Exception exception = new RuntimeException("Unexpected error");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Unexpected error");
    }

    @Test
    void onMethodArgumentNotValidException_returnsBadRequest() {
        // given
        final BindingResult bindingResult = mock(BindingResult.class);
        final FieldError fieldError = new FieldError("object", "name", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        final MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("must not be blank");
    }

    @Test
    void onMethodArgumentNotValidException_multipleErrors_allReturned() {
        // given
        final BindingResult bindingResult = mock(BindingResult.class);
        final FieldError fieldError1 = new FieldError("object", "name", "must not be blank");
        final FieldError fieldError2 = new FieldError("object", "description", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        final MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactlyInAnyOrder("must not be blank", "must not be blank");
    }

    @Test
    void onMethodArgumentNotValidException_noFieldErrors_emptyErrorsList() {
        // given
        final BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());
        final MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void onConstraintViolationException_multipleViolations_allReturned() {
        // given
        final ConstraintViolation<Object> violation1 = mock(ConstraintViolation.class);
        when(violation1.getMessage()).thenReturn("must not be null");
        final ConstraintViolation<Object> violation2 = mock(ConstraintViolation.class);
        when(violation2.getMessage()).thenReturn("must be positive");
        final ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation1, violation2));

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConstraintViolationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactlyInAnyOrder("must not be null", "must be positive");
    }

    @Test
    void onException_nullMessage_emptyErrorsList() {
        // given
        final Exception exception = new RuntimeException((String) null);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).isEmpty();
    }
}
