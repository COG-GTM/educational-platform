package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_withMessage_exceptionCreated() {
        // given
        final String message = "Course with uuid: abc not found";

        // when
        final ResourceNotFoundException sut = new ResourceNotFoundException(message);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
        assertThat(sut.getMessage()).isEqualTo("Course with uuid: abc not found");
    }

    @Test
    void constructor_nullMessage_exceptionCreated() {
        // when
        final ResourceNotFoundException sut = new ResourceNotFoundException(null);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
        assertThat(sut.getMessage()).isNull();
    }
}
