package com.educational.platform.course.enrollments.archive;

import java.util.UUID;

/**
 * Represents command for restoring an archived enrollment to the active learning list.
 */
public record RestoreCourseEnrollmentCommand(UUID uuid) {

}
