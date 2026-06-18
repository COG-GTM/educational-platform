package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class CourseDTOResultTransformerTest {

    private final CourseDTOResultTransformer sut = new CourseDTOResultTransformer();

    @Test
    void aliasToIndexMap_mapsEachAliasToItsPositionalIndex() {
        // given - the transformer reads tuple values by column alias, so it must first build a
        // lookup from each alias to the position it occupies in the projection
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN};

        // when
        final Map<String, Integer> aliasToIndexMap = sut.aliasToIndexMap(aliases);

        // then - insertion order is preserved (LinkedHashMap) and every alias points at its index
        assertThat(aliasToIndexMap).containsExactly(
                entry(CourseDTO.UUID_COLUMN, 0),
                entry(CourseDTO.NAME_COLUMN, 1),
                entry(CourseDTO.DESCRIPTION_COLUMN, 2),
                entry(CourseDTO.NUMBER_OF_STUDENTS_COLUMN, 3));
    }

    @Test
    void aliasToIndexMap_emptyAliases_returnsEmptyMap() {
        // given - a boundary projection with no columns

        // when
        final Map<String, Integer> aliasToIndexMap = sut.aliasToIndexMap(new String[0]);

        // then
        assertThat(aliasToIndexMap).isEmpty();
    }

    @Test
    void transformTuple_mapsCourseScalarFieldsFromTupleByAlias() {
        // given - a single joined row whose aliases are deliberately out of order to prove the
        // scalar course fields are resolved through the alias -> index map and not positionally
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Object[] tuple = {
                "Intro to DDD",
                new NumberOfStudents(7),
                uuid,
                "course description",
                "Lecture"};
        final String[] aliases = {
                CourseDTO.NAME_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CourseDTO.UUID_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CurriculumItemDTO.TYPE};

        // when
        final CourseDTO dto = sut.transformTuple(tuple, aliases);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.name()).isEqualTo("Intro to DDD");
        assertThat(dto.description()).isEqualTo("course description");
        assertThat(dto.numberOfStudents()).isEqualTo(7);
        assertThat(dto.curriculumItems()).isNotNull();
    }

    @Test
    void transformTuple_repeatedCourseUuid_collapsesRowsIntoSingleCourseDTOInstance() {
        // given - a left join emits one row per curriculum item, so the same course uuid appears on
        // several rows; the transformer must dedupe them onto the first CourseDTO it built for that uuid
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE};
        final Object[] firstRow = {uuid, "Original Name", "first", new NumberOfStudents(1), "Lecture"};
        final Object[] secondRow = {uuid, "Ignored Name", "second", new NumberOfStudents(2), "Quiz"};

        // when
        final CourseDTO first = sut.transformTuple(firstRow, aliases);
        final CourseDTO second = sut.transformTuple(secondRow, aliases);

        // then - the second row reuses the existing instance, so its scalar values never overwrite the first's
        assertThat(second).isSameAs(first);
        assertThat(second.name()).isEqualTo("Original Name");
        assertThat(second.numberOfStudents()).isEqualTo(1);
    }

    @Test
    void transformTuple_distinctCourseUuids_produceDistinctCourseDTOInstances() {
        // given - rows for two different courses must not be merged
        final UUID firstUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final UUID secondUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440004");
        final String[] aliases = {
                CourseDTO.UUID_COLUMN,
                CourseDTO.NAME_COLUMN,
                CourseDTO.DESCRIPTION_COLUMN,
                CourseDTO.NUMBER_OF_STUDENTS_COLUMN,
                CurriculumItemDTO.TYPE};
        final Object[] firstRow = {firstUuid, "First", "first", new NumberOfStudents(1), "Lecture"};
        final Object[] secondRow = {secondUuid, "Second", "second", new NumberOfStudents(2), "Lecture"};

        // when
        final CourseDTO first = sut.transformTuple(firstRow, aliases);
        final CourseDTO second = sut.transformTuple(secondRow, aliases);

        // then
        assertThat(second).isNotSameAs(first);
        assertThat(first.uuid()).isEqualTo(firstUuid);
        assertThat(second.uuid()).isEqualTo(secondUuid);
    }
}
