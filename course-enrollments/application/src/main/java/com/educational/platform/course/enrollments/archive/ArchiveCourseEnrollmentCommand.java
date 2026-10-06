package com.educational.platform.course.enrollments.archive;

import java.util.UUID;

/**
 * Represents command for archiving the enrollment (hiding it from the active learning list).
 */
public record ArchiveCourseEnrollmentCommand(UUID uuid) {

}
