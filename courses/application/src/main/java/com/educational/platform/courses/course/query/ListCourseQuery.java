package com.educational.platform.courses.course.query;

/**
 * Represents course query for retrieving a page of courses.
 */
public record ListCourseQuery(int page, int size) {

	public static final int MAX_PAGE_SIZE = 100;
	public static final int DEFAULT_PAGE_SIZE = 20;

	public ListCourseQuery {
		if (page < 0) {
			page = 0;
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			size = DEFAULT_PAGE_SIZE;
		}
	}

	public ListCourseQuery() {
		this(0, DEFAULT_PAGE_SIZE);
	}
}
