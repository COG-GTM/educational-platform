package com.educational.platform.course.enrollments.progress;

import java.util.UUID;

/**
 * Represents command for marking a lecture of the enrollment as completed or not completed.
 */
public record UpdateLectureProgressCommand(UUID enrollmentUuid, UUID lectureUuid, boolean completed) {

}
