package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void constructor_singleErrorString_wrapsInList() {
        // when
        final ErrorResponse response = new ErrorResponse("error message");

        // then
        assertThat(response.errors()).containsExactly("error message");
    }

    @Test
    void constructor_nullString_returnsEmptyList() {
        // when
        final ErrorResponse response = new ErrorResponse((String) null);

        // then
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void constructor_errorList_preservesList() {
        // when
        final ErrorResponse response = new ErrorResponse(List.of("error1", "error2"));

        // then
        assertThat(response.errors()).containsExactly("error1", "error2");
    }
}
