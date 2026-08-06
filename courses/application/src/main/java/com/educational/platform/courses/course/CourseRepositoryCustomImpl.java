package com.educational.platform.courses.course;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import com.educational.platform.courses.course.catalog.CourseCatalogItemDTO;
import com.educational.platform.courses.course.catalog.CourseCatalogPageDTO;
import com.educational.platform.courses.course.catalog.CourseCatalogQuery;
import com.educational.platform.courses.course.catalog.CourseCatalogSort;

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
	public CourseCatalogPageDTO findCatalog(CourseCatalogQuery query) {
		final StringBuilder conditions = new StringBuilder(" where course.publishStatus = :publishStatus");
		final Map<String, Object> parameters = new HashMap<>();
		parameters.put("publishStatus", PublishStatus.PUBLISHED);

		if (query.search() != null && !query.search().isBlank()) {
			conditions.append(" and (lower(course.name) like :search or lower(course.description) like :search)");
			parameters.put("search", "%" + query.search().toLowerCase() + "%");
		}
		if (query.category() != null && !query.category().isBlank()) {
			conditions.append(" and course.category = :category");
			parameters.put("category", query.category());
		}
		if (query.teacher() != null && !query.teacher().isBlank()) {
			conditions.append(" and teacher.username = :teacher");
			parameters.put("teacher", query.teacher());
		}
		if (query.minRating() != null) {
			conditions.append(" and course.rating.rating >= :minRating");
			parameters.put("minRating", query.minRating());
		}

		final String from = " from com.educational.platform.courses.course.Course course"
				+ " join com.educational.platform.courses.teacher.Teacher teacher on course.teacher = teacher.id";

		final TypedQuery<Long> countQuery = entityManager.createQuery("select count(course)" + from + conditions, Long.class);
		parameters.forEach(countQuery::setParameter);
		final long totalElements = countQuery.getSingleResult();

		final TypedQuery<CourseCatalogItemDTO> itemsQuery = entityManager.createQuery(
				"select new com.educational.platform.courses.course.catalog.CourseCatalogItemDTO("
						+ "course.uuid, course.name, course.description, teacher.username, course.rating.rating,"
						+ " course.numberOfStudents.number, course.category, course.createdDate)"
						+ from + conditions + orderBy(query.sort()),
				CourseCatalogItemDTO.class);
		parameters.forEach(itemsQuery::setParameter);
		final List<CourseCatalogItemDTO> items = itemsQuery
				.setFirstResult(query.page() * query.size())
				.setMaxResults(query.size())
				.getResultList();

		final int totalPages = (int) Math.ceil((double) totalElements / query.size());
		return new CourseCatalogPageDTO(items, query.page(), query.size(), totalElements, totalPages);
	}

	private String orderBy(CourseCatalogSort sort) {
		return switch (sort == null ? CourseCatalogSort.NEWEST : sort) {
			case RATING -> " order by course.rating.rating desc, course.id desc";
			case POPULARITY -> " order by course.numberOfStudents.number desc, course.id desc";
			case NEWEST -> " order by course.createdDate desc, course.id desc";
		};
	}
}
