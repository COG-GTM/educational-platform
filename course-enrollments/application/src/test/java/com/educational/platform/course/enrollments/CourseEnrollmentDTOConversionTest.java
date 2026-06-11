package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CourseEnrollmentDTO} secondary constructor that accepts a domain
 * {@link CompletionStatus} and converts it to {@link CompletionStatusDTO}.
 */
public class CourseEnrollmentDTOConversionTest {

    @Test
    void constructor_withDomainStatus_inProgress_convertsToDTO() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UUID courseUuid = UUID.randomUUID();

        // when
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(uuid, courseUuid, "student", CompletionStatus.IN_PROGRESS);

        // then
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.course()).isEqualTo(courseUuid);
        assertThat(dto.student()).isEqualTo("student");
    }

    @Test
    void constructor_withDomainStatus_completed_convertsToDTO() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UUID courseUuid = UUID.randomUUID();

        // when
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(uuid, courseUuid, "student", CompletionStatus.COMPLETED);

        // then
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }

    @Test
    void constructor_withDTOStatus_directConstruction() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UUID courseUuid = UUID.randomUUID();

        // when
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(uuid, courseUuid, "student", CompletionStatusDTO.COMPLETED);

        // then
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }

    @Test
    void equalInstances_withSameValues() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UUID courseUuid = UUID.randomUUID();

        // then
        assertThat(new CourseEnrollmentDTO(uuid, courseUuid, "s", CompletionStatusDTO.IN_PROGRESS))
                .isEqualTo(new CourseEnrollmentDTO(uuid, courseUuid, "s", CompletionStatusDTO.IN_PROGRESS));
    }

    @Test
    void differentCompletionStatus_notEqual() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UUID courseUuid = UUID.randomUUID();

        // then
        assertThat(new CourseEnrollmentDTO(uuid, courseUuid, "s", CompletionStatusDTO.IN_PROGRESS))
                .isNotEqualTo(new CourseEnrollmentDTO(uuid, courseUuid, "s", CompletionStatusDTO.COMPLETED));
    }
}
