package com.educational.platform.course.reviews;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents course review dto.
 */
public record CourseReviewDTO(UUID uuid, UUID course, String username, String comment, double rating, LocalDateTime createdDate) {

}
