package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the convenience constructor of {@link CourseEnrollmentDTO} that accepts
 * a {@link CompletionStatus} and maps it to {@link CompletionStatusDTO}.
 */
public class CourseEnrollmentDTOConvenienceConstructorTest {

    @Test
    void convenienceConstructor_inProgress_mapsToDTOInProgress() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UUID courseUuid = UUID.randomUUID();

        // when
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(uuid, courseUuid, "student1", CompletionStatus.IN_PROGRESS);

        // then
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.course()).isEqualTo(courseUuid);
        assertThat(dto.student()).isEqualTo("student1");
    }

    @Test
    void convenienceConstructor_completed_mapsToDTOCompleted() {
        // given
        final UUID uuid = UUID.randomUUID();
        final UUID courseUuid = UUID.randomUUID();

        // when
        final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(uuid, courseUuid, "student2", CompletionStatus.COMPLETED);

        // then
        assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
    }
}
