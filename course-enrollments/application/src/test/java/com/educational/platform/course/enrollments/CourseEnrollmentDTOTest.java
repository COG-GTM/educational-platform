package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID COURSE_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

    @Test
    void constructor_withDTOStatus_keepsProvidedValues() {
        // when
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(UUID_VALUE, COURSE_VALUE, "student", CompletionStatusDTO.COMPLETED);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.course()).isEqualTo(COURSE_VALUE);
        assertThat(dto.student()).isEqualTo("student");
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }

    @Test
    void constructor_withDomainStatus_convertsStatusToDTO() {
        // when
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(UUID_VALUE, COURSE_VALUE, "student", CompletionStatus.IN_PROGRESS);

        // then
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }
}
