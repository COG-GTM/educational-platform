package com.educational.platform.web.handler;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
}
