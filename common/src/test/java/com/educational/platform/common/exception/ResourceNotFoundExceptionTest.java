package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void getMessage_returnsProvidedMessage() {
        // given
        final ResourceNotFoundException sut = new ResourceNotFoundException("course not found");

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message).isEqualTo("course not found");
    }

    @Test
    void isRuntimeException() {
        // given
        final ResourceNotFoundException sut = new ResourceNotFoundException("course not found");

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
    }
}
