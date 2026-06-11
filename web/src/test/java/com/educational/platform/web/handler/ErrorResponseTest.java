package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorResponseTest {

    @Test
    void singleErrorConstructor_wrapsErrorInList() {
        final ErrorResponse response = new ErrorResponse("error");
        assertThat(response.errors()).containsExactly("error");
    }

    @Test
    void singleErrorConstructor_nullError_producesEmptyList() {
        final ErrorResponse response = new ErrorResponse((String) null);
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void listConstructor_keepsProvidedErrors() {
        final ErrorResponse response = new ErrorResponse(List.of("first", "second"));
        assertThat(response.errors()).containsExactly("first", "second");
    }
}
