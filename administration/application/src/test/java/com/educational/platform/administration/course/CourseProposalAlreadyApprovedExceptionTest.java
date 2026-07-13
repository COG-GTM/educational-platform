package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseProposalAlreadyApprovedExceptionTest {

    @Test
    void getMessage_containsUuidAndReason() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalAlreadyApprovedException sut = new CourseProposalAlreadyApprovedException(uuid);

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message)
                .isEqualTo("Course Proposal with uuid = " + uuid + " cannot be approved, course proposal was already approved");
    }
}
