package com.educational.platform.course.enrollments.query;

import java.util.UUID;

/**
 * Represents query for retrieving the current student's enrollment to the course.
 */
public record CourseEnrollmentByCourseQuery(UUID courseUuid) {

}
