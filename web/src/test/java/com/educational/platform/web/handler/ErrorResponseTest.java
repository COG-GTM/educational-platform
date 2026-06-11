package com.educational.platform.web.handler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorResponseTest {

    @Test
    void constructor_withSingleMessage_wrapsSingletonList() {
        // when
        final ErrorResponse response = new ErrorResponse("some error");

        // then
        assertThat(response.errors()).containsExactly("some error");
    }

    @Test
    void constructor_withNullMessage_returnsEmptyList() {
        // when
        final ErrorResponse response = new ErrorResponse((String) null);

        // then
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void constructor_withList_keepsProvidedList() {
        // given
        final List<String> errors = List.of("error1", "error2");

        // when
        final ErrorResponse response = new ErrorResponse(errors);

        // then
        assertThat(response.errors()).containsExactly("error1", "error2");
    }

    @Test
    void constructor_withEmptyList_returnsEmptyErrors() {
        // when
        final ErrorResponse response = new ErrorResponse(List.of());

        // then
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void equals_sameErrors_areEqual() {
        // given
        final ErrorResponse first = new ErrorResponse("msg");
        final ErrorResponse second = new ErrorResponse("msg");

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentErrors_notEqual() {
        // given
        final ErrorResponse first = new ErrorResponse("msg1");
        final ErrorResponse second = new ErrorResponse("msg2");

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
