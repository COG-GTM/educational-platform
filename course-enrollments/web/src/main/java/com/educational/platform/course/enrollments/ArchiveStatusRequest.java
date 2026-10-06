package com.educational.platform.course.enrollments;

/**
 * Represents request for archiving ({@code true}) or restoring ({@code false}) an enrollment.
 */
public record ArchiveStatusRequest(boolean archived) {

}
