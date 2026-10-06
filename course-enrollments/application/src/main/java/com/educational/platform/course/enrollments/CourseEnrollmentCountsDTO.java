package com.educational.platform.course.enrollments;

/**
 * Represents how many enrollments the student has in each dashboard group.
 */
public record CourseEnrollmentCountsDTO(long inProgress, long completed, long archived) {

}
