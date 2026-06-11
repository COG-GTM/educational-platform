package com.educational.platform.courses.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the curriculum-item branching in {@link CourseDTOResultTransformer#transformTuple}.
 * <p>
 * The transformer checks {@code aliasToIndexMap.get(CurriculumItemDTO.TYPE)} to decide
 * whether to add a {@link LectureDTO} or {@link QuizDTO} to the course DTO.
 */
public class CourseDTOResultTransformerCurriculumTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    private CourseDTOResultTransformer sut;

    @BeforeEach
    void setUp() {
        sut = new CourseDTOResultTransformer();
    }

    @Test
    void transformTuple_lectureType_addsCurriculumItem() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE, CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER, LectureDTO.TEXT
        };
        final Object[] tuple = {
                UUID_VALUE, "name", "description", new NumberOfStudents(5),
                "Lecture", "Lecture Title", "Lecture Desc", 1, "Lecture Content"
        };

        // when
        final CourseDTO dto = sut.transformTuple(tuple, aliases);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
    }

    @Test
    void transformTuple_quizType_addsCurriculumItem() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE, CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER
        };
        final Object[] tuple = {
                UUID_VALUE, "name", "description", new NumberOfStudents(2),
                "Quiz", "Quiz Title", "Quiz Desc", 1
        };

        // when
        final CourseDTO dto = sut.transformTuple(tuple, aliases);

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.name()).isEqualTo("name");
    }

    @Test
    void transformTuple_unknownType_noCurriculumItemAdded() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {
                UUID_VALUE, "name", "description", new NumberOfStudents(1), "UnknownType"
        };

        // when
        final CourseDTO dto = sut.transformTuple(tuple, aliases);

        // then
        assertThat(dto.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_multipleTuplesForSameCourse_accumulates() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN, CourseDTO.NAME_COLUMN, CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN, CurriculumItemDTO.TYPE
        };
        final Object[] tuple1 = {
                UUID_VALUE, "name", "desc", new NumberOfStudents(1), "Other"
        };
        final Object[] tuple2 = {
                UUID_VALUE, "name", "desc", new NumberOfStudents(1), "Other"
        };

        // when
        sut.transformTuple(tuple1, aliases);
        final CourseDTO dto = sut.transformTuple(tuple2, aliases);

        // then — same UUID → same DTO returned, curriculumItems stays empty for unknown types
        assertThat(dto.curriculumItems()).isEmpty();
    }
}
