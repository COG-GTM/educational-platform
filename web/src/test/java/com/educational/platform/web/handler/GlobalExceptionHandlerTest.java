package com.educational.platform.web.handler;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void onRelatedResourceIsNotResolvedException_returnsBadRequest() {
        final ResponseEntity<ErrorResponse> response =
                sut.onRelatedResourceIsNotResolvedException(new RelatedResourceIsNotResolvedException("not resolved"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsExactly("not resolved");
    }

    @Test
    void onResourceNotFoundException_returnsNotFound() {
        final ResponseEntity<ErrorResponse> response =
                sut.onResourceNotFoundException(new ResourceNotFoundException("not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errors()).containsExactly("not found");
    }

    @Test
    void onUnprocessableEntityException_returnsUnprocessableEntity() {
        final ResponseEntity<ErrorResponse> response =
                sut.onUnprocessableEntityException(new UnprocessableEntityException("unprocessable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().errors()).containsExactly("unprocessable");
    }

    @Test
    void onConstraintViolationException_returnsBadRequest() {
        final ResponseEntity<ErrorResponse> response =
                sut.onConstraintViolationException(new ConstraintViolationException(Collections.emptySet()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).isEmpty();
    }

    @Test
    void onException_returnsInternalServerError() {
        final ResponseEntity<ErrorResponse> response = sut.onException(new Exception("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errors()).containsExactly("boom");
    }
}
