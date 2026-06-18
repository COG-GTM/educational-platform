package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseLightDTOTest {

    @Test
    void numberOfStudentsConstructor_unwrapsValueObject() {
        // given - CourseRepository.list() projects rows through this constructor (JPQL "SELECT new ..."),
        // so the NumberOfStudents value object must be unwrapped to its primitive number for the DTO
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseLightDTO dto = new CourseLightDTO(uuid, "Course Name", "Course Description",
                new NumberOfStudents(7));

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.name()).isEqualTo("Course Name");
        assertThat(dto.description()).isEqualTo("Course Description");
        assertThat(dto.numberOfStudents()).isEqualTo(7);
    }

    @Test
    void numberOfStudentsConstructor_zeroStudents_unwrapsToZero() {
        // given - a freshly created course has zero students; the projection must surface 0, not reject it
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseLightDTO dto = new CourseLightDTO(uuid, "Course Name", "Course Description",
                new NumberOfStudents(0));

        // then
        assertThat(dto.numberOfStudents()).isZero();
    }
}
