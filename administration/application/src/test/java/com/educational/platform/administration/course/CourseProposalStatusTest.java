package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalStatusTest {

    @Test
    void toDTO_waitingForApproval_mapsToWaitingForApprovalDTO() {
        // when
        final CourseProposalStatusDTO dto = CourseProposalStatus.WAITING_FOR_APPROVAL.toDTO();

        // then
        assertThat(dto).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_declined_mapsToDeclinedDTO() {
        // when
        final CourseProposalStatusDTO dto = CourseProposalStatus.DECLINED.toDTO();

        // then
        assertThat(dto).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void toDTO_approved_mapsToApprovedDTO() {
        // when
        final CourseProposalStatusDTO dto = CourseProposalStatus.APPROVED.toDTO();

        // then
        assertThat(dto).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @ParameterizedTest
    @EnumSource(CourseProposalStatus.class)
    void toDTO_anyStatus_returnsNonNullDTOWithMatchingName(CourseProposalStatus status) {
        // when
        final CourseProposalStatusDTO dto = status.toDTO();

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo(status.name());
    }
}
