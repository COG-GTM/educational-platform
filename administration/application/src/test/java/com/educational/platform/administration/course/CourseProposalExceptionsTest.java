package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalExceptionsTest {

    @Test
    void alreadyApprovedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalAlreadyApprovedException exception = new CourseProposalAlreadyApprovedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("cannot be approved")
                .contains("already approved");
    }

    @Test
    void alreadyDeclinedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseProposalAlreadyDeclinedException exception = new CourseProposalAlreadyDeclinedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("cannot be declined")
                .contains("already declined");
    }

    @Test
    void alreadyApprovedException_isRuntimeException() {
        final CourseProposalAlreadyApprovedException exception =
                new CourseProposalAlreadyApprovedException(UUID.randomUUID());
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void alreadyDeclinedException_isRuntimeException() {
        final CourseProposalAlreadyDeclinedException exception =
                new CourseProposalAlreadyDeclinedException(UUID.randomUUID());
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
