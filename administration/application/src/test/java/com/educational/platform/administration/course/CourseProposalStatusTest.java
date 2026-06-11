package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalStatusTest {

    @Test
    void toDTO_waitingForApproval_correspondingDTO() {
        // when
        final CourseProposalStatusDTO result = CourseProposalStatus.WAITING_FOR_APPROVAL.toDTO();

        // then
        assertThat(result).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_declined_correspondingDTO() {
        // when
        final CourseProposalStatusDTO result = CourseProposalStatus.DECLINED.toDTO();

        // then
        assertThat(result).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void toDTO_approved_correspondingDTO() {
        // when
        final CourseProposalStatusDTO result = CourseProposalStatus.APPROVED.toDTO();

        // then
        assertThat(result).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }
}
