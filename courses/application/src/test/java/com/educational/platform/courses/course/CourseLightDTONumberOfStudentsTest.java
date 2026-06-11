package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseLightDTONumberOfStudentsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void convenienceConstructor_positiveStudents_mapsNumber() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "Course", "Description", new NumberOfStudents(42));

        // then
        assertThat(dto.numberOfStudents()).isEqualTo(42);
    }

    @Test
    void convenienceConstructor_largeNumber_mapsCorrectly() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "Course", "Desc", new NumberOfStudents(10000));

        // then
        assertThat(dto.numberOfStudents()).isEqualTo(10000);
    }

    @Test
    void primaryAndConvenienceConstructor_sameNumber_areEqual() {
        // given
        final CourseLightDTO fromPrimary = new CourseLightDTO(UUID_VALUE, "Course", "Desc", 7);
        final CourseLightDTO fromConvenience = new CourseLightDTO(UUID_VALUE, "Course", "Desc", new NumberOfStudents(7));

        // then
        assertThat(fromPrimary).isEqualTo(fromConvenience);
        assertThat(fromPrimary.hashCode()).isEqualTo(fromConvenience.hashCode());
    }
}
