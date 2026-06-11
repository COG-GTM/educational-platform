package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseProposalStatusDTOTest {

    @Test
    void values_containsExpectedStatuses() {
        assertThat(CourseProposalStatusDTO.values())
                .containsExactlyInAnyOrder(
                        CourseProposalStatusDTO.WAITING_FOR_APPROVAL,
                        CourseProposalStatusDTO.APPROVED,
                        CourseProposalStatusDTO.DECLINED
                );
    }

    @Test
    void values_count_matchesCourseProposalStatusCount() {
        assertThat(CourseProposalStatusDTO.values())
                .hasSameSizeAs(CourseProposalStatus.values());
    }

    @Test
    void valueOf_waitingForApproval_returnsCorrectValue() {
        assertThat(CourseProposalStatusDTO.valueOf("WAITING_FOR_APPROVAL"))
                .isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void valueOf_approved_returnsCorrectValue() {
        assertThat(CourseProposalStatusDTO.valueOf("APPROVED"))
                .isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void valueOf_declined_returnsCorrectValue() {
        assertThat(CourseProposalStatusDTO.valueOf("DECLINED"))
                .isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void valueOf_invalidName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> CourseProposalStatusDTO.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
