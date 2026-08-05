package com.educational.platform.courses.course.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ListCourseQueryTest {

	@Test
	void constructor_noArgs_defaults() {
		// when
		var query = new ListCourseQuery();

		// then
		assertThat(query.page()).isZero();
		assertThat(query.size()).isEqualTo(ListCourseQuery.DEFAULT_PAGE_SIZE);
	}

	@Test
	void constructor_validPageAndSize_preserved() {
		// when
		var query = new ListCourseQuery(3, 50);

		// then
		assertThat(query.page()).isEqualTo(3);
		assertThat(query.size()).isEqualTo(50);
	}

	@Test
	void constructor_negativePage_clampedToZero() {
		// when
		var query = new ListCourseQuery(-1, 10);

		// then
		assertThat(query.page()).isZero();
		assertThat(query.size()).isEqualTo(10);
	}

	@Test
	void constructor_zeroSize_fallsBackToDefault() {
		// when
		var query = new ListCourseQuery(0, 0);

		// then
		assertThat(query.size()).isEqualTo(ListCourseQuery.DEFAULT_PAGE_SIZE);
	}

	@Test
	void constructor_negativeSize_fallsBackToDefault() {
		// when
		var query = new ListCourseQuery(0, -5);

		// then
		assertThat(query.size()).isEqualTo(ListCourseQuery.DEFAULT_PAGE_SIZE);
	}

	@Test
	void constructor_sizeAboveMax_clampedToMax() {
		// when
		var query = new ListCourseQuery(0, ListCourseQuery.MAX_PAGE_SIZE + 1);

		// then
		assertThat(query.size()).isEqualTo(ListCourseQuery.MAX_PAGE_SIZE);
	}

	@Test
	void constructor_sizeAtBoundaries_preserved() {
		// when
		var min = new ListCourseQuery(0, 1);
		var max = new ListCourseQuery(0, ListCourseQuery.MAX_PAGE_SIZE);

		// then
		assertThat(min.size()).isEqualTo(1);
		assertThat(max.size()).isEqualTo(ListCourseQuery.MAX_PAGE_SIZE);
	}
}
