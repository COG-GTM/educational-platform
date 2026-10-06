package com.educational.platform.course.enrollments.query;

import jakarta.annotation.Nullable;

/**
 * Represents course enrollments query for retrieving a page of the current student's enrollments.
 *
 * @param status group to filter by, {@literal null} returns every enrollment.
 * @param page   zero based page index.
 * @param size   page size.
 */
public record ListCourseEnrollmentsQuery(@Nullable EnrollmentStatusFilter status, int page, int size) {

    public static final int DEFAULT_SIZE = 12;
    public static final int MAX_SIZE = 50;

    public ListCourseEnrollmentsQuery() {
        this(null, 0, DEFAULT_SIZE);
    }

    public ListCourseEnrollmentsQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
