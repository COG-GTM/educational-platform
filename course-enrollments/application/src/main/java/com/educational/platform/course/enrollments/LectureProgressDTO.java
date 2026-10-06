package com.educational.platform.course.enrollments;

import java.util.UUID;

/**
 * Represents a lecture of the enrolled course together with the student's completion state.
 */
public record LectureProgressDTO(UUID uuid, String title, Integer serialNumber, boolean completed) {

}
