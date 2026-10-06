package com.educational.platform.course.enrollments;

import java.util.List;

/**
 * Represents a page of the student's enrollments.
 */
public record CourseEnrollmentPageDTO(List<CourseEnrollmentDTO> items, int page, int size, long totalElements,
                                      int totalPages, CourseEnrollmentCountsDTO counts) {

}
