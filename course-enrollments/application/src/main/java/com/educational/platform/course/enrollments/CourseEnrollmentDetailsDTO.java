package com.educational.platform.course.enrollments;

import java.util.List;

/**
 * Represents the enrollment together with the curriculum the student works through.
 */
public record CourseEnrollmentDetailsDTO(CourseEnrollmentDTO enrollment, List<LectureProgressDTO> lectures) {

}
