package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnprocessableEntityExceptionTest {

    @Test
    void getMessage_returnsProvidedMessage() {
        // given
        final UnprocessableEntityException sut = new UnprocessableEntityException("cannot process entity");

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message).isEqualTo("cannot process entity");
    }

    @Test
    void isRuntimeException() {
        // given
        final UnprocessableEntityException sut = new UnprocessableEntityException("cannot process entity");

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
    }
}
