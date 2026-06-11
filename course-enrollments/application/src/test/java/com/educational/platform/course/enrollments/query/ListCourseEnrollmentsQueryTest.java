package com.educational.platform.course.enrollments.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ListCourseEnrollmentsQueryTest {

	@Test
	void constructor_createsInstance() {
		// when
		final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();

		// then
		assertThat(query).isNotNull();
	}

	@Test
	void equals_twoInstances_areEqual() {
		// when / then
		assertThat(new ListCourseEnrollmentsQuery()).isEqualTo(new ListCourseEnrollmentsQuery());
	}
}
