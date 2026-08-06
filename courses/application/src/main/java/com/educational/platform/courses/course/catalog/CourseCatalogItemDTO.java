package com.educational.platform.courses.course.catalog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a published course as shown in the catalog.
 */
public record CourseCatalogItemDTO(UUID uuid, String name, String description, String teacherName, double rating,
								   int numberOfStudents, String category, LocalDateTime createdDate) {

}
