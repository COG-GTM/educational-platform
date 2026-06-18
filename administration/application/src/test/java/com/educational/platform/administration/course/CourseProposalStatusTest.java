package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalStatusTest {

    @Test
    void toDTO_waitingForApproval_mappedToWaitingForApprovalDTO() {
        // when
        final CourseProposalStatusDTO dto = CourseProposalStatus.WAITING_FOR_APPROVAL.toDTO();

        // then
        assertThat(dto).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_approved_mappedToApprovedDTO() {
        // when
        final CourseProposalStatusDTO dto = CourseProposalStatus.APPROVED.toDTO();

        // then
        assertThat(dto).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_declined_mappedToDeclinedDTO() {
        // when
        final CourseProposalStatusDTO dto = CourseProposalStatus.DECLINED.toDTO();

        // then
        assertThat(dto).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }
}
