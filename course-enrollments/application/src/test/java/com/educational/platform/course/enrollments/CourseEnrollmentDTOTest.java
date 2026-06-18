package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentDTOTest {

    @Test
    void completionStatusConstructor_inProgress_mapsToInProgressDTO() {
        // given - CourseEnrollmentRepository projects rows through this constructor (JPQL "SELECT new ..."),
        // so the CompletionStatus domain enum must be mapped to its DTO counterpart
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID course = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseEnrollmentDTO dto =
                new CourseEnrollmentDTO(uuid, course, "student", CompletionStatus.IN_PROGRESS);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.course()).isEqualTo(course);
        assertThat(dto.student()).isEqualTo("student");
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void completionStatusConstructor_completed_mapsToCompletedDTO() {
        // given - a completed enrollment must surface as the COMPLETED DTO through the projection constructor
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final UUID course = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");

        // when
        final CourseEnrollmentDTO dto =
                new CourseEnrollmentDTO(uuid, course, "student", CompletionStatus.COMPLETED);

        // then
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
