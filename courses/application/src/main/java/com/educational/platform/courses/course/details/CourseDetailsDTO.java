package com.educational.platform.courses.course.details;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a published course with all data shown on the course detail page.
 */
public record CourseDetailsDTO(UUID uuid, String name, String description, String category, String teacherName,
							   double rating, int numberOfStudents, LocalDateTime publishedDate,
							   List<CurriculumItemSummaryDTO> curriculumItems) {

	public CourseDetailsDTO(UUID uuid, String name, String description, String category, String teacherName,
							double rating, int numberOfStudents, LocalDateTime publishedDate) {
		this(uuid, name, description, category, teacherName, rating, numberOfStudents, publishedDate, List.of());
	}

	public CourseDetailsDTO withCurriculumItems(List<CurriculumItemSummaryDTO> items) {
		return new CourseDetailsDTO(uuid, name, description, category, teacherName, rating, numberOfStudents, publishedDate, items);
	}
}
