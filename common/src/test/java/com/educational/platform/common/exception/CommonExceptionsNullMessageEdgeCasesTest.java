package com.educational.platform.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies null-message handling for all common exception types.
 * {@link ResourceNotFoundException} null case is covered separately;
 * this test covers the remaining two exception classes.
 */
public class CommonExceptionsNullMessageEdgeCasesTest {

    @Test
    void relatedResourceIsNotResolvedException_nullMessage_allowed() {
        // when
        final RelatedResourceIsNotResolvedException exception =
                new RelatedResourceIsNotResolvedException(null);

        // then
        assertThat(exception.getMessage()).isNull();
    }

    @Test
    void unprocessableEntityException_nullMessage_allowed() {
        // when
        final UnprocessableEntityException exception =
                new UnprocessableEntityException(null);

        // then
        assertThat(exception.getMessage()).isNull();
    }

    @Test
    void relatedResourceIsNotResolvedException_emptyMessage_preserved() {
        // when
        final RelatedResourceIsNotResolvedException exception =
                new RelatedResourceIsNotResolvedException("");

        // then
        assertThat(exception.getMessage()).isEmpty();
    }

    @Test
    void unprocessableEntityException_emptyMessage_preserved() {
        // when
        final UnprocessableEntityException exception =
                new UnprocessableEntityException("");

        // then
        assertThat(exception.getMessage()).isEmpty();
    }
}
