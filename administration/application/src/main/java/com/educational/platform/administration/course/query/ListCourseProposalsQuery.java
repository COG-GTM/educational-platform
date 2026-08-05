package com.educational.platform.administration.course.query;

/**
 * Represents list course proposals query.
 */
public record ListCourseProposalsQuery(int page, int size) {

	public static final int MAX_PAGE_SIZE = 100;
	public static final int DEFAULT_PAGE_SIZE = 20;

	public ListCourseProposalsQuery {
		if (page < 0) {
			page = 0;
		}
		if (size < 1) {
			size = DEFAULT_PAGE_SIZE;
		} else if (size > MAX_PAGE_SIZE) {
			size = MAX_PAGE_SIZE;
		}
	}

	public ListCourseProposalsQuery() {
		this(0, DEFAULT_PAGE_SIZE);
	}
}
