package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorResponseTest {

    @Test
    void stringConstructor_nonNullMessage_wrapsMessageInSingletonList() {
        // given - the single-message error handlers (404/422/related-resource) build the body via this
        // convenience constructor, so a non-null message must surface as a one-element errors list
        final ErrorResponse response = new ErrorResponse("resource not found");

        // then
        assertThat(response.errors()).containsExactly("resource not found");
    }

    @Test
    void stringConstructor_nullMessage_emptyErrorsList() {
        // given - an exception without a message must map to an empty list rather than [null], so clients
        // always receive a well-formed errors array
        final ErrorResponse response = new ErrorResponse((String) null);

        // then
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void listConstructor_preservesProvidedErrorsInOrder() {
        // given - the validation handlers collect every field/violation message and pass the list straight
        // through the canonical constructor, so the supplied errors must be preserved verbatim and in order
        final ErrorResponse response = new ErrorResponse(List.of("first error", "second error"));

        // then
        assertThat(response.errors()).containsExactly("first error", "second error");
    }
}
