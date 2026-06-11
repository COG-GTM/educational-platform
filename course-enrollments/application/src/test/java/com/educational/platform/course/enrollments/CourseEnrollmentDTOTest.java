package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentDTOTest {

	@Test
	void constructor_withCompletionStatusDTO_storesAllFields() {
		// given
		final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

		// when
		final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatusDTO.IN_PROGRESS);

		// then
		assertThat(dto.uuid()).isEqualTo(enrollmentUuid);
		assertThat(dto.course()).isEqualTo(courseUuid);
		assertThat(dto.student()).isEqualTo("student");
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}

	@Test
	void constructor_withDomainCompletionStatus_convertsToDTO() {
		// given
		final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

		// when
		final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatus.IN_PROGRESS);

		// then
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}

	@Test
	void constructor_withDomainCompletedStatus_convertsToCompletedDTO() {
		// given
		final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

		// when
		final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(enrollmentUuid, courseUuid, "student", CompletionStatus.COMPLETED);

		// then
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.COMPLETED);
	}

	@Test
	void constructor_nullFields_acceptsNulls() {
		// when
		final CourseEnrollmentDTO dto = new CourseEnrollmentDTO(null, null, null, CompletionStatusDTO.IN_PROGRESS);

		// then
		assertThat(dto.uuid()).isNull();
		assertThat(dto.course()).isNull();
		assertThat(dto.student()).isNull();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}
}
