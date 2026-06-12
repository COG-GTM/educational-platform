package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedResourceIsNotResolvedExceptionTest {

    @Test
    void constructor_withMessage_exceptionCreated() {
        // given
        final String message = "Course cannot be found by uuid = 123";

        // when
        final RelatedResourceIsNotResolvedException sut = new RelatedResourceIsNotResolvedException(message);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
        assertThat(sut.getMessage()).isEqualTo("Course cannot be found by uuid = 123");
    }

    @Test
    void constructor_nullMessage_exceptionCreated() {
        // when
        final RelatedResourceIsNotResolvedException sut = new RelatedResourceIsNotResolvedException(null);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
        assertThat(sut.getMessage()).isNull();
    }
}
