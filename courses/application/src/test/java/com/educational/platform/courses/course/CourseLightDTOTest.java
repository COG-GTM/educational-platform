package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseLightDTOTest {

    @Test
    void constructor_withPrimitiveValues_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "Java Course", "Learn Java", 42);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("Java Course");
        assertThat(sut.description()).isEqualTo("Learn Java");
        assertThat(sut.numberOfStudents()).isEqualTo(42);
    }

    @Test
    void constructor_withNumberOfStudents_convertsToInt() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final NumberOfStudents numberOfStudents = new NumberOfStudents(10);

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "Java Course", "Learn Java", numberOfStudents);

        // then
        assertThat(sut.numberOfStudents()).isEqualTo(10);
    }

    @Test
    void constructor_zeroStudents_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final NumberOfStudents numberOfStudents = new NumberOfStudents(0);

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "Java Course", "Learn Java", numberOfStudents);

        // then
        assertThat(sut.numberOfStudents()).isZero();
    }
}
