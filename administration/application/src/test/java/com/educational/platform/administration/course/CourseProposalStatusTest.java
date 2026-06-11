package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseProposalStatusTest {

    @Test
    void toDTO_waitingForApproval_correspondingDTO() {
        assertThat(CourseProposalStatus.WAITING_FOR_APPROVAL.toDTO())
                .isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_approved_correspondingDTO() {
        assertThat(CourseProposalStatus.APPROVED.toDTO())
                .isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_declined_correspondingDTO() {
        assertThat(CourseProposalStatus.DECLINED.toDTO())
                .isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @ParameterizedTest
    @EnumSource(CourseProposalStatus.class)
    void toDTO_allValues_nonNull(CourseProposalStatus status) {
        assertThat(status.toDTO()).isNotNull();
    }

    @Test
    void values_count_matchesStatusDTOCount() {
        assertThat(CourseProposalStatus.values())
                .hasSameSizeAs(CourseProposalStatusDTO.values());
    }

    @ParameterizedTest
    @EnumSource(CourseProposalStatus.class)
    void name_matchesCorrespondingStatusDTOName(CourseProposalStatus status) {
        assertThat(status.toDTO().name()).isEqualTo(status.name());
    }

    @Test
    void valueOf_invalidName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> CourseProposalStatus.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allValues_mapToDistinctDTOValues() {
        final var dtoValues = java.util.Arrays.stream(CourseProposalStatus.values())
                .map(CourseProposalStatus::toDTO)
                .toList();
        assertThat(dtoValues).doesNotHaveDuplicates();
    }
}
