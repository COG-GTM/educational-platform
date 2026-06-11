package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseLightDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void primaryConstructor_storesAllFields() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "name", "description", 5);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
        assertThat(dto.description()).isEqualTo("description");
        assertThat(dto.numberOfStudents()).isEqualTo(5);
    }

    @Test
    void equalityAndHashCode_sameValues_equal() {
        final CourseLightDTO first = new CourseLightDTO(UUID_VALUE, "name", "description", 5);
        final CourseLightDTO second = new CourseLightDTO(UUID_VALUE, "name", "description", 5);
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equalityAndHashCode_differentValues_notEqual() {
        final CourseLightDTO first = new CourseLightDTO(UUID_VALUE, "name", "description", 5);
        final CourseLightDTO second = new CourseLightDTO(UUID_VALUE, "name", "description", 10);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void convenienceConstructor_zeroStudents_returnsZero() {
        // when
        final CourseLightDTO dto = new CourseLightDTO(UUID_VALUE, "name", "desc", new NumberOfStudents(0));

        // then
        assertThat(dto.numberOfStudents()).isZero();
    }
}
