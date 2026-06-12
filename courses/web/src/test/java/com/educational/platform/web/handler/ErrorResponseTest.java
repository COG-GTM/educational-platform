package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void constructor_singleErrorMessage_wrappedInList() {
        // given / when
        final ErrorResponse sut = new ErrorResponse("Something went wrong");

        // then
        assertThat(sut.errors()).containsExactly("Something went wrong");
    }

    @Test
    void constructor_nullErrorMessage_emptyList() {
        // given / when
        final ErrorResponse sut = new ErrorResponse((String) null);

        // then
        assertThat(sut.errors()).isEmpty();
    }

    @Test
    void constructor_errorList_preservedAsIs() {
        // given
        final List<String> errors = List.of("Error 1", "Error 2");

        // when
        final ErrorResponse sut = new ErrorResponse(errors);

        // then
        assertThat(sut.errors()).containsExactly("Error 1", "Error 2");
    }

    @Test
    void constructor_emptyErrorList_emptyList() {
        // given / when
        final ErrorResponse sut = new ErrorResponse(List.of());

        // then
        assertThat(sut.errors()).isEmpty();
    }
}
