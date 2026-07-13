package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedResourceIsNotResolvedExceptionTest {

    @Test
    void getMessage_returnsProvidedMessage() {
        // given
        final RelatedResourceIsNotResolvedException sut =
                new RelatedResourceIsNotResolvedException("related resource is not resolved");

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message).isEqualTo("related resource is not resolved");
    }

    @Test
    void isRuntimeException() {
        // given
        final RelatedResourceIsNotResolvedException sut =
                new RelatedResourceIsNotResolvedException("related resource is not resolved");

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
    }
}
