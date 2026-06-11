package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the convenience constructor of {@link CourseLightDTO} that accepts
 * {@link NumberOfStudents} and maps it to an int field.
 */
public class CourseLightDTOConstructorTest {

    @Test
    void convenienceConstructor_mapsNumberOfStudentsToInt() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        final CourseLightDTO dto = new CourseLightDTO(uuid, "Math", "Basic math", new NumberOfStudents(15));

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.name()).isEqualTo("Math");
        assertThat(dto.description()).isEqualTo("Basic math");
        assertThat(dto.numberOfStudents()).isEqualTo(15);
    }

    @Test
    void convenienceConstructor_zeroStudents() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        final CourseLightDTO dto = new CourseLightDTO(uuid, "Empty", "No students", new NumberOfStudents(0));

        // then
        assertThat(dto.numberOfStudents()).isZero();
    }

    @Test
    void canonicalConstructor_storesIntDirectly() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        final CourseLightDTO dto = new CourseLightDTO(uuid, "Physics", "Intro", 100);

        // then
        assertThat(dto.numberOfStudents()).isEqualTo(100);
    }
}
