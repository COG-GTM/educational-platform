package com.educational.platform.courses.course;

import org.hibernate.query.TupleTransformer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents course dto result transformer.
 */
public class CourseDTOResultTransformer implements TupleTransformer<CourseDTO> {

    private final Map<UUID, CourseDTO> courseDTOMap = new LinkedHashMap<>();

    @Override
    public CourseDTO transformTuple(
            Object[] tuple, String[] aliases
    ) {

        Map<String, Integer> aliasToIndexMap = aliasToIndexMap(aliases);

        UUID uuid = UUID.fromString(String.valueOf(tuple[aliasToIndexMap.get(CourseDTO.UUID_COLUMN)]));

        CourseDTO courseDTO = courseDTOMap.computeIfAbsent(
                uuid,
                id -> new CourseDTO(tuple, aliasToIndexMap)
        );
        String type = typeName(tuple[aliasToIndexMap.get(CurriculumItemDTO.TYPE)]);
        if ("Lecture".equals(type)) {
            courseDTO.curriculumItems().add(new LectureDTO(itemUuid(tuple, aliasToIndexMap), tuple, aliasToIndexMap));
        } else if ("Quiz".equals(type)) {
            courseDTO.curriculumItems().add(new QuizDTO(itemUuid(tuple, aliasToIndexMap), tuple, aliasToIndexMap));
        }
        return courseDTO;
    }

    private UUID itemUuid(Object[] tuple, Map<String, Integer> aliasToIndexMap) {
        return UUID.fromString(String.valueOf(tuple[aliasToIndexMap.get(CurriculumItemDTO.UUID_COLUMN)]));
    }

    private String typeName(Object type) {
        if (type == null) {
            return null;
        }
        if (type instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        }
        return type.toString();
    }

    public Map<String, Integer> aliasToIndexMap(
            String[] aliases
    ) {

        Map<String, Integer> aliasToIndexMap = new LinkedHashMap<>();

        for (int i = 0; i < aliases.length; i++) {
            aliasToIndexMap.put(aliases[i], i);
        }

        return aliasToIndexMap;
    }
}
