package com.educational.platform.web.handler;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.common.exception.UnprocessableEntityException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerEdgeCasesTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void onException_nullMessage_returnsEmptyErrorsList() {
        // given
        final Exception exception = new Exception((String) null);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void onConstraintViolationException_withViolations_returnsBadRequestWithMessages() {
        // given
        final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("must not be null");
        final ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        // when
        final ResponseEntity<ErrorResponse> response = sut.onConstraintViolationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsExactly("must not be null");
    }

    @Test
    void onResourceNotFoundException_nullMessage_returnsEmptyErrorsList() {
        // given
        final ResourceNotFoundException exception = new ResourceNotFoundException(null);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onResourceNotFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errors()).isEmpty();
    }

    @Test
    void onUnprocessableEntityException_nullMessage_returnsEmptyErrorsList() {
        // given
        final UnprocessableEntityException exception = new UnprocessableEntityException(null);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onUnprocessableEntityException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().errors()).isEmpty();
    }

    @Test
    void onRelatedResourceIsNotResolvedException_nullMessage_returnsEmptyErrorsList() {
        // given
        final RelatedResourceIsNotResolvedException exception = new RelatedResourceIsNotResolvedException(null);

        // when
        final ResponseEntity<ErrorResponse> response = sut.onRelatedResourceIsNotResolvedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).isEmpty();
    }
}
