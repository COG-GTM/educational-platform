package com.educational.platform.courses.course;

import org.hibernate.query.TupleTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CourseDTOResultTransformerTest {

    private CourseDTOResultTransformer sut;

    @BeforeEach
    void setUp() {
        sut = new CourseDTOResultTransformer();
    }

    // --- aliasToIndexMap tests ---

    @Test
    void aliasToIndexMap_emptyAliases_returnsEmptyMap() {
        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(new String[]{});

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void aliasToIndexMap_singleAlias_returnsSingleMapping() {
        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(new String[]{"course_name"});

        // then
        assertThat(result).hasSize(1);
        assertThat(result).containsEntry("course_name", 0);
    }

    @Test
    void aliasToIndexMap_multipleAliases_returnsCorrectMappings() {
        // given
        final String[] aliases = {"course_uuid", "course_name", "course_description"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).containsEntry("course_uuid", 0);
        assertThat(result).containsEntry("course_name", 1);
        assertThat(result).containsEntry("course_description", 2);
    }

    @Test
    void aliasToIndexMap_preservesInsertionOrder() {
        // given
        final String[] aliases = {"z_column", "a_column", "m_column"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).isInstanceOf(LinkedHashMap.class);
        assertThat(result.keySet()).containsExactly("z_column", "a_column", "m_column");
    }

    @Test
    void aliasToIndexMap_knownCourseColumns_allMappedCorrectly() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE,
                CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER
        };

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).hasSize(8);
        assertThat(result.get(CourseDTO.UUID_COLUMN)).isEqualTo(0);
        assertThat(result.get(CourseDTO.NAME_COLUMN)).isEqualTo(1);
        assertThat(result.get(CourseDTO.DESCRIPTION_COLUMN)).isEqualTo(2);
        assertThat(result.get(CourseDTO.NUMBER_OF_STUDENTS_COLUMN)).isEqualTo(3);
        assertThat(result.get(CurriculumItemDTO.TYPE)).isEqualTo(4);
    }

    @Test
    void aliasToIndexMap_duplicateAliases_lastIndexWins() {
        // given
        final String[] aliases = {"col_a", "col_b", "col_a"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("col_a", 2);
        assertThat(result).containsEntry("col_b", 1);
    }

    @Test
    void aliasToIndexMap_nullAlias_nullKeyMapped() {
        // given
        final String[] aliases = {"col_a", null, "col_b"};

        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(aliases);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).containsEntry(null, 1);
    }

    @Test
    void aliasToIndexMap_calledMultipleTimes_independentResults() {
        // given
        final String[] aliases1 = {"col_a"};
        final String[] aliases2 = {"col_x", "col_y"};

        // when
        final Map<String, Integer> result1 = sut.aliasToIndexMap(aliases1);
        final Map<String, Integer> result2 = sut.aliasToIndexMap(aliases2);

        // then
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(2);
        assertThat(result1).isNotSameAs(result2);
    }

    // --- transformTuple tests ---

    @Test
    void transformTuple_validCourseData_returnsCourseDTO() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {
                courseUuid,
                "Course Name",
                "Course Description",
                new NumberOfStudents(10),
                "Other"
        };

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result).isNotNull();
        assertThat(result.uuid()).isEqualTo(courseUuid);
        assertThat(result.name()).isEqualTo("Course Name");
        assertThat(result.description()).isEqualTo("Course Description");
        assertThat(result.numberOfStudents()).isEqualTo(10);
    }

    @Test
    void transformTuple_sameCourseUuidCalledTwice_returnsSameInstance() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple1 = {courseUuid, "Name", "Desc", new NumberOfStudents(5), "Other"};
        final Object[] tuple2 = {courseUuid, "Name", "Desc", new NumberOfStudents(5), "Other"};

        // when
        final CourseDTO result1 = sut.transformTuple(tuple1, aliases);
        final CourseDTO result2 = sut.transformTuple(tuple2, aliases);

        // then
        assertThat(result1).isSameAs(result2);
    }

    @Test
    void transformTuple_differentCourseUuids_returnsDifferentInstances() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple1 = {uuid1, "Name1", "Desc1", new NumberOfStudents(1), "Other"};
        final Object[] tuple2 = {uuid2, "Name2", "Desc2", new NumberOfStudents(2), "Other"};

        // when
        final CourseDTO result1 = sut.transformTuple(tuple1, aliases);
        final CourseDTO result2 = sut.transformTuple(tuple2, aliases);

        // then
        assertThat(result1).isNotSameAs(result2);
        assertThat(result1.uuid()).isEqualTo(uuid1);
        assertThat(result2.uuid()).isEqualTo(uuid2);
    }

    @Test
    void transformTuple_courseWithZeroStudents_returnedCorrectly() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(0), "Other"};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result.numberOfStudents()).isEqualTo(0);
    }

    @Test
    void transformTuple_courseDTOHasEmptyCurriculumItemsListInitially() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(5), "UnknownType"};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result.curriculumItems()).isNotNull();
    }

    // --- structural tests ---

    @Test
    void implementsTupleTransformerInterface() {
        // then
        assertThat(TupleTransformer.class).isAssignableFrom(CourseDTOResultTransformer.class);
    }

    @Test
    void aliasToIndexMapMethodIsPublic() throws NoSuchMethodException {
        // when
        final var method = CourseDTOResultTransformer.class.getDeclaredMethod("aliasToIndexMap", String[].class);

        // then
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void transformTupleMethodIsPublic() throws NoSuchMethodException {
        // when
        final var method = CourseDTOResultTransformer.class.getDeclaredMethod("transformTuple", Object[].class, String[].class);

        // then
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void courseDTOMapFieldIsPrivateAndFinal() throws NoSuchFieldException {
        // when
        final Field field = CourseDTOResultTransformer.class.getDeclaredField("courseDTOMap");

        // then
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void newInstanceHasEmptyInternalState() {
        // given
        final CourseDTOResultTransformer transformer = new CourseDTOResultTransformer();

        // when — first call creates a new CourseDTO
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {uuid, "Name", "Desc", new NumberOfStudents(0), "Other"};
        final CourseDTO result = transformer.transformTuple(tuple, aliases);

        // then
        assertThat(result).isNotNull();
        assertThat(result.uuid()).isEqualTo(uuid);
    }

    @Test
    void aliasToIndexMap_returnsLinkedHashMap() {
        // when
        final Map<String, Integer> result = sut.aliasToIndexMap(new String[]{"a", "b"});

        // then
        assertThat(result).isExactlyInstanceOf(LinkedHashMap.class);
    }

    @Test
    void transformTuple_nullNameAndDescription_returnedAsIs() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {courseUuid, null, null, new NumberOfStudents(0), "Other"};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result.name()).isNull();
        assertThat(result.description()).isNull();
    }

    // --- type branching tests ---

    @Test
    void transformTuple_lectureType_curriculumItemsNotPopulated() {
        // given — the type-index comparison means curriculum items are never added
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE,
                CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER,
                LectureDTO.TEXT
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(5),
                "Lecture", "Lecture Title", "Lecture Desc", 1, "Some text"};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result).isNotNull();
        assertThat(result.uuid()).isEqualTo(courseUuid);
        assertThat(result.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_quizType_curriculumItemsNotPopulated() {
        // given — the type-index comparison means curriculum items are never added
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE,
                CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(3),
                "Quiz", "Quiz Title", "Quiz Desc", 2};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result).isNotNull();
        assertThat(result.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_unknownType_curriculumItemsEmpty() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(0), "SomeOtherType"};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_multipleTuplesSameUuid_courseInstanceCached() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple1 = {courseUuid, "Name", "Desc", new NumberOfStudents(1), "Type1"};
        final Object[] tuple2 = {courseUuid, "Updated Name", "Updated Desc", new NumberOfStudents(99), "Type2"};

        // when
        final CourseDTO result1 = sut.transformTuple(tuple1, aliases);
        final CourseDTO result2 = sut.transformTuple(tuple2, aliases);

        // then — same UUID returns cached instance; second tuple's data is ignored
        assertThat(result1).isSameAs(result2);
        assertThat(result1.name()).isEqualTo("Name");
    }

    @Test
    void transformTuple_differentInstances_doNotShareState() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(5), "Other"};

        // when
        final CourseDTO result1 = sut.transformTuple(tuple, aliases);
        final CourseDTOResultTransformer otherInstance = new CourseDTOResultTransformer();
        final CourseDTO result2 = otherInstance.transformTuple(tuple, aliases);

        // then — different transformer instances produce different CourseDTO objects
        assertThat(result1).isNotSameAs(result2);
        assertThat(result1.uuid()).isEqualTo(result2.uuid());
    }

    @Test
    void transformTuple_multipleDifferentUuids_allCachedIndependently() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
        final UUID uuid3 = UUID.fromString("323e4567-e89b-12d3-a456-426655440003");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };

        // when
        final CourseDTO r1 = sut.transformTuple(new Object[]{uuid1, "A", "DA", new NumberOfStudents(1), "X"}, aliases);
        final CourseDTO r2 = sut.transformTuple(new Object[]{uuid2, "B", "DB", new NumberOfStudents(2), "X"}, aliases);
        final CourseDTO r3 = sut.transformTuple(new Object[]{uuid3, "C", "DC", new NumberOfStudents(3), "X"}, aliases);
        final CourseDTO r1Again = sut.transformTuple(new Object[]{uuid1, "Z", "DZ", new NumberOfStudents(99), "X"}, aliases);

        // then
        assertThat(r1).isNotSameAs(r2);
        assertThat(r2).isNotSameAs(r3);
        assertThat(r1).isSameAs(r1Again);
        assertThat(r1.name()).isEqualTo("A");
    }

    @Test
    void transformTuple_typeIndexNeverMatchesLiteralLectureOrQuiz() {
        // given — type alias at index 4, its toString() is "4", never "Lecture"
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE,
                CurriculumItemDTO.TITLE,
                CurriculumItemDTO.DESCRIPTION,
                CurriculumItemDTO.SERIAL_NUMBER,
                LectureDTO.TEXT
        };

        // when — first tuple is Lecture, second is Quiz
        sut.transformTuple(new Object[]{courseUuid, "N", "D", new NumberOfStudents(0),
                "Lecture", "LT", "LD", 1, "text"}, aliases);
        final CourseDTO result = sut.transformTuple(new Object[]{courseUuid, "N", "D", new NumberOfStudents(0),
                "Quiz", "QT", "QD", 2, null}, aliases);

        // then — neither branch is reached; curriculum items stay empty
        assertThat(result.curriculumItems()).isEmpty();
    }

    @Test
    void transformTuple_uuidStringConversion_usesStringValueOf() {
        // given — UUID.fromString(String.valueOf(tuple[...])) is the production path
        final UUID courseUuid = UUID.fromString("abcdef01-2345-6789-abcd-ef0123456789");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(0), "Other"};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result.uuid()).isEqualTo(courseUuid);
    }

    @Test
    void transformTuple_nullUuidInTuple_throwsException() {
        // given
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {null, "Name", "Desc", new NumberOfStudents(0), "Other"};

        // when / then — UUID.fromString(String.valueOf(null)) → "null" → IllegalArgumentException
        assertThatThrownBy(() -> sut.transformTuple(tuple, aliases))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transformTuple_numberOfStudentsExtractedFromValueObject() {
        // given
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(42), "Other"};

        // when
        final CourseDTO result = sut.transformTuple(tuple, aliases);

        // then
        assertThat(result.numberOfStudents()).isEqualTo(42);
    }

    @Test
    void transformTuple_missingTypeAlias_throwsNullPointerException() {
        // given — aliases do not include CurriculumItemDTO.TYPE
        final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN
        };
        final Object[] tuple = {courseUuid, "Name", "Desc", new NumberOfStudents(0)};

        // when / then — aliasToIndexMap.get(CurriculumItemDTO.TYPE) returns null → NPE
        assertThatThrownBy(() -> sut.transformTuple(tuple, aliases))
                .isInstanceOf(NullPointerException.class);
    }
}
