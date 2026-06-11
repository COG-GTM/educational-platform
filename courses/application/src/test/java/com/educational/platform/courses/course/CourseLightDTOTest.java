package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CourseLightDTOTest {

    @Test
    void canonicalConstructor_allFieldsPopulated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "Course Name", "Description", 10);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("Course Name");
        assertThat(sut.description()).isEqualTo("Description");
        assertThat(sut.numberOfStudents()).isEqualTo(10);
    }

    @Test
    void numberOfStudentsConstructor_extractsNumberFromValueObject() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final NumberOfStudents numberOfStudents = new NumberOfStudents(25);

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "Name", "Desc", numberOfStudents);

        // then
        assertThat(sut.numberOfStudents()).isEqualTo(25);
    }

    @Test
    void numberOfStudentsConstructor_zeroStudents_extractedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "Name", "Desc", new NumberOfStudents(0));

        // then
        assertThat(sut.numberOfStudents()).isEqualTo(0);
    }

    @Test
    void numberOfStudentsConstructor_nullValueObject_throwsNPE() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when / then
        assertThatThrownBy(() -> new CourseLightDTO(uuid, "Name", "Desc", (NumberOfStudents) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canonicalConstructor_nullFields_storedCorrectly() {
        // when
        final CourseLightDTO sut = new CourseLightDTO(null, null, null, 0);

        // then
        assertThat(sut.uuid()).isNull();
        assertThat(sut.name()).isNull();
        assertThat(sut.description()).isNull();
        assertThat(sut.numberOfStudents()).isEqualTo(0);
    }

    @Test
    void equalInstances_areEqual() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseLightDTO a = new CourseLightDTO(uuid, "Name", "Desc", 5);
        final CourseLightDTO b = new CourseLightDTO(uuid, "Name", "Desc", 5);

        // then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentInstances_areNotEqual() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final CourseLightDTO a = new CourseLightDTO(uuid1, "Name", "Desc", 5);
        final CourseLightDTO b = new CourseLightDTO(uuid2, "Name", "Desc", 5);

        // then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void numberOfStudentsConstructor_preservesAllOtherFields() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "My Course", "My Description", new NumberOfStudents(100));

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.name()).isEqualTo("My Course");
        assertThat(sut.description()).isEqualTo("My Description");
        assertThat(sut.numberOfStudents()).isEqualTo(100);
    }

    @Test
    void canonicalConstructor_emptyStrings_storedCorrectly() {
        // when
        final CourseLightDTO sut = new CourseLightDTO(null, "", "", 0);

        // then
        assertThat(sut.name()).isEmpty();
        assertThat(sut.description()).isEmpty();
    }

    @Test
    void numberOfStudentsConstructor_maxIntStudents_extractedCorrectly() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseLightDTO sut = new CourseLightDTO(uuid, "Name", "Desc", new NumberOfStudents(Integer.MAX_VALUE));

        // then
        assertThat(sut.numberOfStudents()).isEqualTo(Integer.MAX_VALUE);
    }
}
