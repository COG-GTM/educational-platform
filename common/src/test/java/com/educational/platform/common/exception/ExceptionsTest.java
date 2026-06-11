package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionsTest {

    @Test
    void resourceNotFoundException_keepsMessage() {
        final ResourceNotFoundException exception = new ResourceNotFoundException("not found");
        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("not found");
    }

    @Test
    void relatedResourceIsNotResolvedException_keepsMessage() {
        final RelatedResourceIsNotResolvedException exception = new RelatedResourceIsNotResolvedException("not resolved");
        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("not resolved");
    }

    @Test
    void unprocessableEntityException_keepsMessage() {
        final UnprocessableEntityException exception = new UnprocessableEntityException("unprocessable");
        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("unprocessable");
    }
}
