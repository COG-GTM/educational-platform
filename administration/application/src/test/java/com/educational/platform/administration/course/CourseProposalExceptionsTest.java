package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalExceptionsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void alreadyApprovedException_messageContainsUuidAndReason() {
        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("already approved");
    }

    @Test
    void alreadyDeclinedException_messageContainsUuidAndReason() {
        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(UUID_VALUE);

        // then
        assertThat(exception.getMessage())
                .contains(UUID_VALUE.toString())
                .contains("already declined");
    }
}
