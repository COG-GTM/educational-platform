package com.educational.platform.courses.course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;

public class CourseRepositoryCustomImpl implements CourseRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional(readOnly = true)
	@Override
	@SuppressWarnings("unchecked")
	public Optional<CourseDTO> findDTOByUuid(UUID uuid) {
		var results = (List<CourseDTO>) entityManager
				.createQuery(
						"select course.uuid as course_uuid, course.name as course_name, course.description as course_description, course.numberOfStudents as course_numberOfStudents, "
								+ " curriculumItems.serialNumber as curriculumItems_serialNumber, curriculumItems.uuid as curriculumItems_uuid, curriculumItems.title as curriculumItems_title, curriculumItems.description as curriculumItems_description, curriculumItems.class as curriculumItems_type, treat(curriculumItems as com.educational.platform.courses.course.Lecture).content as text"
								+ " from com.educational.platform.courses.course.Course course left join course.curriculumItems curriculumItems WHERE course.uuid = :uuid")
				.setParameter("uuid", uuid)
				.unwrap(org.hibernate.query.Query.class)
				.setTupleTransformer(new CourseDTOResultTransformer())
				.getResultList();

		return results.stream().findFirst();
	}
}
