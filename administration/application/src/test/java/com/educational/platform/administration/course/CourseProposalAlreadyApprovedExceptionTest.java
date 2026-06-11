package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseProposalAlreadyApprovedExceptionTest {

    @Test
    void getMessage_containsUuid() {
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
}
