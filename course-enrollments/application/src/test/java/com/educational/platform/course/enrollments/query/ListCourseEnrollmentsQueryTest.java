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

	@Test
	void hashCode_twoInstances_sameHashCode() {
		// when / then
		assertThat(new ListCourseEnrollmentsQuery().hashCode())
				.isEqualTo(new ListCourseEnrollmentsQuery().hashCode());
	}

	@Test
	void toString_returnsNonNull() {
		// when
		final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();

		// then
		assertThat(query.toString()).isNotNull();
	}

	@Test
	void equals_null_isNotEqual() {
		// given
		final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();

		// then
		assertThat(query).isNotEqualTo(null);
	}
}
