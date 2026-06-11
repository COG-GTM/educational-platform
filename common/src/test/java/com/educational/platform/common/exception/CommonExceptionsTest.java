package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonExceptionsTest {

    @Test
    void resourceNotFoundException_messagePreserved() {
        // when
        final ResourceNotFoundException exception = new ResourceNotFoundException("Course not found");

        // then
        assertThat(exception.getMessage()).isEqualTo("Course not found");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void relatedResourceIsNotResolvedException_messagePreserved() {
        // when
        final RelatedResourceIsNotResolvedException exception =
                new RelatedResourceIsNotResolvedException("Cannot resolve related resource");

        // then
        assertThat(exception.getMessage()).isEqualTo("Cannot resolve related resource");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void unprocessableEntityException_messagePreserved() {
        // when
        final UnprocessableEntityException exception =
                new UnprocessableEntityException("Username already taken");

        // then
        assertThat(exception.getMessage()).isEqualTo("Username already taken");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void resourceNotFoundException_nullMessage_allowed() {
        // when
        final ResourceNotFoundException exception = new ResourceNotFoundException(null);

        // then
        assertThat(exception.getMessage()).isNull();
    }
}
