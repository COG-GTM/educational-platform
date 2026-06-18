package com.educational.platform.courses.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class CourseRepositoryCustomImpl implements CourseRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Optional<CourseDTO> findDTOByUuid(UUID uuid) {
		CourseDTO result = (CourseDTO) entityManager
				.createQuery(
						"select course.uuid as course_uuid, course.name as course_name, course.description as course_description, course.numberOfStudents as course_numberOfStudents, "
								+ " curriculumItems.serialNumber as curriculumItems_serialNumber, curriculumItems.uuid as curriculumItems_uuid, curriculumItems.title as curriculumItems_title, curriculumItems.description as curriculumItems_description, curriculumItems.class as curriculumItems_type"
								+ " from com.educational.platform.courses.course.Course course left join course.curriculumItems curriculumItems WHERE course.uuid = :uuid")
				.setParameter("uuid", uuid)
				.unwrap(org.hibernate.query.Query.class)
				.setTupleTransformer(new CourseDTOResultTransformer())
				.getSingleResult();

		return Optional.ofNullable(result);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<CourseLightDTO> searchByKeyword(String keyword) {
		final String sql = "SELECT uuid, name, description, number_of_students FROM course "
				+ "WHERE name LIKE '%" + keyword + "%' OR description LIKE '%" + keyword + "%'";

		final List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();

		final List<CourseLightDTO> results = new ArrayList<>();
		for (Object[] row : rows) {
			results.add(new CourseLightDTO((UUID) row[0], (String) row[1], (String) row[2],
					((Number) row[3]).intValue()));
		}

		return results;
	}
}
