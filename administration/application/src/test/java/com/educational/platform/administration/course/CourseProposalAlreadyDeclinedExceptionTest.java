package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseProposalAlreadyDeclinedExceptionTest {

    @Test
    void getMessage_containsUuidAndReason() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalAlreadyDeclinedException sut = new CourseProposalAlreadyDeclinedException(uuid);

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message)
                .isEqualTo("Course Proposal with uuid = " + uuid + " cannot be declined, course proposal was already declined");
    }
}
