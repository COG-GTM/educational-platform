package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentDTOTest {

    @Test
    void constructor_withCompletionStatus_convertsToDTOStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseEnrollmentDTO sut = new CourseEnrollmentDTO(uuid, courseUuid, "student", CompletionStatus.IN_PROGRESS);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.course()).isEqualTo(courseUuid);
        assertThat(sut.student()).isEqualTo("student");
        assertThat(sut.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void constructor_withCompletedStatus_convertsToDTOStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseEnrollmentDTO sut = new CourseEnrollmentDTO(uuid, courseUuid, "student", CompletionStatus.COMPLETED);

        // then
        assertThat(sut.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
