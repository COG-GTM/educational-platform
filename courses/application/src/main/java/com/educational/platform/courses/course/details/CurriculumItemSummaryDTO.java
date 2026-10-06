package com.educational.platform.courses.course.details;

import java.util.UUID;

/**
 * Represents a curriculum item (lecture or quiz) in the curriculum outline.
 */
public record CurriculumItemSummaryDTO(UUID uuid, String title, String description, Integer serialNumber, String type) {

}
