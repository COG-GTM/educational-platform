package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalStatusDTOTest {

    @Test
    void values_allStatusesExist() {
        // given
        final CourseProposalStatusDTO[] values = CourseProposalStatusDTO.values();

        // when / then
        assertThat(values).containsExactlyInAnyOrder(
                CourseProposalStatusDTO.WAITING_FOR_APPROVAL,
                CourseProposalStatusDTO.DECLINED,
                CourseProposalStatusDTO.APPROVED
        );
    }

    @Test
    void valueOf_waitingForApproval_correctValue() {
        // when
        final CourseProposalStatusDTO status = CourseProposalStatusDTO.valueOf("WAITING_FOR_APPROVAL");

        // then
        assertThat(status).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void valueOf_declined_correctValue() {
        // when
        final CourseProposalStatusDTO status = CourseProposalStatusDTO.valueOf("DECLINED");

        // then
        assertThat(status).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void valueOf_approved_correctValue() {
        // when
        final CourseProposalStatusDTO status = CourseProposalStatusDTO.valueOf("APPROVED");

        // then
        assertThat(status).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }
}
