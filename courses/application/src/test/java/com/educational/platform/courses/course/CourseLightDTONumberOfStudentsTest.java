package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CourseLightDTO} convenience constructor that accepts
 * {@link NumberOfStudents} and unwraps it to a primitive int.
 */
public class CourseLightDTONumberOfStudentsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void convenienceConstructor_positiveStudents_extractsNumber() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "Java 101", "desc", new NumberOfStudents(42));

        // then
        assertThat(dto.numberOfStudents()).isEqualTo(42);
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("Java 101");
    }

    @Test
    void convenienceConstructor_largeStudentCount_extractsNumber() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "n", "d", new NumberOfStudents(100_000));

        // then
        assertThat(dto.numberOfStudents()).isEqualTo(100_000);
    }

    @Test
    void convenienceConstructor_matchesPrimaryConstructor() {
        // given
        final CourseLightDTO fromPrimary = new CourseLightDTO(UUID_VALUE, "name", "desc", 7);
        final CourseLightDTO fromConvenience = new CourseLightDTO(UUID_VALUE, "name", "desc", new NumberOfStudents(7));

        // then
        assertThat(fromConvenience).isEqualTo(fromPrimary);
    }

    @Test
    void differentUuids_notEqual() {
        // given
        final UUID otherUuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final CourseLightDTO first = new CourseLightDTO(UUID_VALUE, "name", "desc", new NumberOfStudents(5));
        final CourseLightDTO second = new CourseLightDTO(otherUuid, "name", "desc", new NumberOfStudents(5));

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
