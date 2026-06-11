package com.educational.platform.courses.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the curriculum-item branching in {@link CourseDTOResultTransformer#transformTuple}.
 * <p>
 * NOTE: The production code on line 29 compares {@code aliasToIndexMap.get(CurriculumItemDTO.TYPE).toString()}
 * (which yields the Integer index, e.g. "4") against the literal "Lecture"/"Quiz".
 * This means the branch never matches and curriculum items are never added to the DTO.
 * The correct comparison would be {@code tuple[aliasToIndexMap.get(CurriculumItemDTO.TYPE)].toString()}.
 * These tests document the current (buggy) behaviour so that any fix can be verified by
 * flipping the assertions.
 */
public class CourseDTOResultTransformerItemTypesTest {

    private static final UUID COURSE_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    private CourseDTOResultTransformer sut;

    @BeforeEach
    void setUp() {
        sut = new CourseDTOResultTransformer();
    }

    @Test
    void transformTuple_lectureType_curriculumItemsEmptyDueToBug() {
        // given
        final String[] aliases = lectureAliases();
        final Object[] tuple = {
                COURSE_UUID, "Course", "desc", new NumberOfStudents(0),
                "Lecture", "L-Title", "L-Desc", 1, "content"
        };

        // when
        final CourseDTO dto = sut.transformTuple(tuple, aliases);

        // then – items are empty because the type-check compares the index, not the value
        assertThat(dto.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_quizType_curriculumItemsEmptyDueToBug() {
        // given
        final String[] aliases = quizAliases();
        final Object[] tuple = {
                COURSE_UUID, "Course", "desc", new NumberOfStudents(0),
                "Quiz", "Q-Title", "Q-Desc", 1
        };

        // when
        final CourseDTO dto = sut.transformTuple(tuple, aliases);

        // then – items are empty because the type-check compares the index, not the value
        assertThat(dto.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_multipleTuplesSameCourse_sameInstanceReturned() {
        // given – same course UUID, two rows
        final String[] aliases = quizAliases();
        final Object[] tuple1 = {
                COURSE_UUID, "Course", "desc", new NumberOfStudents(5),
                "Quiz", "Q1", "Q1-desc", 1
        };
        final Object[] tuple2 = {
                COURSE_UUID, "Course", "desc", new NumberOfStudents(5),
                "Lecture", "L1", "L1-desc", 2
        };

        // when
        final CourseDTO first = sut.transformTuple(tuple1, aliases);
        final CourseDTO second = sut.transformTuple(tuple2, aliases);

        // then – computeIfAbsent returns the same cached DTO
        assertThat(first).isSameAs(second);
        assertThat(first.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_differentCourses_separateDTOs() {
        // given
        final UUID anotherUuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final String[] aliases = quizAliases();
        final Object[] tuple1 = {
                COURSE_UUID, "Course A", "desc-a", new NumberOfStudents(1),
                "Quiz", "Q1", "Q1-desc", 1
        };
        final Object[] tuple2 = {
                anotherUuid, "Course B", "desc-b", new NumberOfStudents(2),
                "Quiz", "Q2", "Q2-desc", 1
        };

        // when
        final CourseDTO dtoA = sut.transformTuple(tuple1, aliases);
        final CourseDTO dtoB = sut.transformTuple(tuple2, aliases);

        // then
        assertThat(dtoA).isNotSameAs(dtoB);
        assertThat(dtoA.name()).isEqualTo("Course A");
        assertThat(dtoB.name()).isEqualTo("Course B");
    }

    @Test
    void aliasToIndexMap_mapsAliasesToCorrectIndices() {
        // given
        final String[] aliases = {"col_a", "col_b", "col_c"};

        // when
        final var map = sut.aliasToIndexMap(aliases);

        // then
        assertThat(map).containsEntry("col_a", 0)
                .containsEntry("col_b", 1)
                .containsEntry("col_c", 2);
    }

    private String[] lectureAliases() {
        return new String[]{
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE, CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER, LectureDTO.TEXT
        };
    }

    private String[] quizAliases() {
        return new String[]{
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE, CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER
        };
    }
}
