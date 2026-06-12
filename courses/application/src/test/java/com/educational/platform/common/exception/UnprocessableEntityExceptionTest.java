package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnprocessableEntityExceptionTest {

    @Test
    void constructor_withMessage_exceptionCreated() {
        // given
        final String message = "Username already exists";

        // when
        final UnprocessableEntityException sut = new UnprocessableEntityException(message);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
        assertThat(sut.getMessage()).isEqualTo("Username already exists");
    }

    @Test
    void constructor_nullMessage_exceptionCreated() {
        // when
        final UnprocessableEntityException sut = new UnprocessableEntityException(null);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
        assertThat(sut.getMessage()).isNull();
    }
}
