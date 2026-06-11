package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CourseProposalStatus#toDTO()} mapping for all enum values.
 */
public class CourseProposalStatusToDTOTest {

    @Test
    void waitingForApproval_mapsToDTO() {
        assertThat(CourseProposalStatus.WAITING_FOR_APPROVAL.toDTO())
                .isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void approved_mapsToDTO() {
        assertThat(CourseProposalStatus.APPROVED.toDTO())
                .isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void declined_mapsToDTO() {
        assertThat(CourseProposalStatus.DECLINED.toDTO())
                .isEqualTo(CourseProposalStatusDTO.DECLINED);
    }
}
