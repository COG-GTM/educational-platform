package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CurriculumItemDTOTest {

    // --- static constants tests ---

    @Test
    void typeConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.TYPE).isEqualTo("curriculumItems_type");
    }

    @Test
    void titleConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.TITLE).isEqualTo("curriculumItems_title");
    }

    @Test
    void descriptionConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.DESCRIPTION).isEqualTo("curriculumItems_description");
    }

    @Test
    void serialNumberConstant_hasExpectedValue() {
        assertThat(CurriculumItemDTO.SERIAL_NUMBER).isEqualTo("curriculumItems_serialNumber");
    }

    // --- abstract class structure ---

    @Test
    void isAbstractClass() {
        assertThat(Modifier.isAbstract(CurriculumItemDTO.class.getModifiers())).isTrue();
    }

    // --- constructor via LectureDTO subtype ---

    @Test
    void constructor_viaLectureDTO_fieldsPopulated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", 3, "Text"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final CurriculumItemDTO sut = new LectureDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(sut).hasFieldOrPropertyWithValue("title", "Title");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "Desc");
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 3);
    }

    // --- constructor via QuizDTO subtype ---

    @Test
    void constructor_viaQuizDTO_fieldsPopulated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Quiz Title", "Quiz Desc", 7};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final CurriculumItemDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(sut).hasFieldOrPropertyWithValue("title", "Quiz Title");
        assertThat(sut).hasFieldOrPropertyWithValue("description", "Quiz Desc");
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 7);
    }

    // --- field visibility ---

    @Test
    void uuidField_isPackagePrivate() throws NoSuchFieldException {
        // when
        final var field = CurriculumItemDTO.class.getDeclaredField("uuid");

        // then — not public, not private, not protected → package-private
        assertThat(Modifier.isPublic(field.getModifiers())).isFalse();
        assertThat(Modifier.isPrivate(field.getModifiers())).isFalse();
        assertThat(Modifier.isProtected(field.getModifiers())).isFalse();
    }

    @Test
    void titleField_isPackagePrivate() throws NoSuchFieldException {
        // when
        final var field = CurriculumItemDTO.class.getDeclaredField("title");

        // then
        assertThat(Modifier.isPublic(field.getModifiers())).isFalse();
        assertThat(Modifier.isPrivate(field.getModifiers())).isFalse();
        assertThat(Modifier.isProtected(field.getModifiers())).isFalse();
    }

    @Test
    void constructor_nullUuid_storedAsNull() {
        // given
        final Object[] tuples = {"Title", "Desc", 1, "text"};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2,
                LectureDTO.TEXT, 3
        );

        // when
        final CurriculumItemDTO sut = new LectureDTO(null, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("uuid", null);
    }

    @Test
    void constructor_nullSerialNumber_storedAsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuples = {"Title", "Desc", null};
        final Map<String, Integer> aliasMap = Map.of(
                CurriculumItemDTO.TITLE, 0,
                CurriculumItemDTO.DESCRIPTION, 1,
                CurriculumItemDTO.SERIAL_NUMBER, 2
        );

        // when
        final CurriculumItemDTO sut = new QuizDTO(uuid, tuples, aliasMap);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", null);
    }
}
