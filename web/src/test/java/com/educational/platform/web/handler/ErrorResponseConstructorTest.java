package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link ErrorResponse} convenience constructors and edge cases.
 */
public class ErrorResponseConstructorTest {

    @Test
    void singleErrorConstructor_wrapsInSingletonList() {
        // when
        final ErrorResponse response = new ErrorResponse("something went wrong");

        // then
        assertThat(response.errors()).containsExactly("something went wrong");
    }

    @Test
    void singleErrorConstructor_nullError_returnsEmptyList() {
        // when
        final ErrorResponse response = new ErrorResponse((String) null);

        // then
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void listConstructor_preservesAllErrors() {
        // given
        final List<String> errors = List.of("error1", "error2", "error3");

        // when
        final ErrorResponse response = new ErrorResponse(errors);

        // then
        assertThat(response.errors()).containsExactly("error1", "error2", "error3");
    }
}
