package com.educational.platform.web.handler;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void onRelatedResourceIsNotResolvedException_returnsBadRequest() {
        // given
        final var exception = new RelatedResourceIsNotResolvedException("Course cannot be found");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onRelatedResourceIsNotResolvedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Course cannot be found");
    }

    @Test
    void onResourceNotFoundException_returnsNotFound() {
        // given
        final var exception = new ResourceNotFoundException("Course with uuid: abc not found");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onResourceNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Course with uuid: abc not found");
    }

    @Test
    void onUnprocessableEntityException_returnsUnprocessableEntity() {
        // given
        final var exception = new UnprocessableEntityException("Username already exists");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onUnprocessableEntityException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Username already exists");
    }

    @Test
    void onException_returnsInternalServerError() {
        // given
        final var exception = new RuntimeException("Unexpected error");

        // when
        final ResponseEntity<ErrorResponse> response = sut.onException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).containsExactly("Unexpected error");
    }

    @Test
    void onResourceNotFoundException_nullMessage_emptyErrorsList() {
        // given
        final var exception = new ResourceNotFoundException(null);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onResourceNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).isEmpty();
    }
}
