package com.educational.platform.courses.course.catalog;

import java.util.List;

/**
 * Represents available filter values for the course catalog.
 */
public record CatalogFacetsDTO(List<String> categories, List<String> teachers) {

}
