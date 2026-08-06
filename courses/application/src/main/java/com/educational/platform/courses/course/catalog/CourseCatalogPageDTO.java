package com.educational.platform.courses.course.catalog;

import java.util.List;

/**
 * Represents a page of catalog courses.
 */
public record CourseCatalogPageDTO(List<CourseCatalogItemDTO> items, int page, int size, long totalElements,
								   int totalPages) {

}
