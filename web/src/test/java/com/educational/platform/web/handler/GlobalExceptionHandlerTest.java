package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void onAccessDeniedException_exceptionWithMessage_forbiddenWithMessage() {
        // given
        final AccessDeniedException exception = new AccessDeniedException("Access Denied");

        // when
        final ResponseEntity<ErrorResponse> result = sut.onAccessDeniedException(exception);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().errors()).containsExactly("Access Denied");
    }

    @Test
    void onAccessDeniedException_exceptionWithoutMessage_forbiddenWithEmptyErrors() {
        // given
        final AccessDeniedException exception = new AccessDeniedException(null);

        // when
        final ResponseEntity<ErrorResponse> result = sut.onAccessDeniedException(exception);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().errors()).isEmpty();
    }

    @Test
    void onException_genericException_internalServerError() {
        // given
        final Exception exception = new IllegalStateException("boom");

        // when
        final ResponseEntity<ErrorResponse> result = sut.onException(exception);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().errors()).containsExactly("boom");
    }

}
