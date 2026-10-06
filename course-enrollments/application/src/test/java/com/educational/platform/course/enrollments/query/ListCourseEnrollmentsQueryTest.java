package com.educational.platform.course.enrollments.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ListCourseEnrollmentsQueryTest {

    @Test
    void create_defaultConstructor_noFilterFirstPageDefaultSize() {
        // when
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery();

        // then
        assertThat(query.status()).isNull();
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(ListCourseEnrollmentsQuery.DEFAULT_SIZE);
    }

    @Test
    void create_validValues_kept() {
        // when
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery(EnrollmentStatusFilter.COMPLETED, 3, 20);

        // then
        assertThat(query.status()).isEqualTo(EnrollmentStatusFilter.COMPLETED);
        assertThat(query.page()).isEqualTo(3);
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void create_negativePage_clampedToFirstPage() {
        // when
        final ListCourseEnrollmentsQuery query = new ListCourseEnrollmentsQuery(null, -5, 10);

        // then
        assertThat(query.page()).isZero();
    }

    @Test
    void create_zeroOrNegativeSize_defaultSize() {
        // when / then
        assertThat(new ListCourseEnrollmentsQuery(null, 0, 0).size()).isEqualTo(ListCourseEnrollmentsQuery.DEFAULT_SIZE);
        assertThat(new ListCourseEnrollmentsQuery(null, 0, -1).size()).isEqualTo(ListCourseEnrollmentsQuery.DEFAULT_SIZE);
    }

    @Test
    void create_sizeAboveMax_cappedToMax() {
        // when / then
        assertThat(new ListCourseEnrollmentsQuery(null, 0, ListCourseEnrollmentsQuery.MAX_SIZE + 1).size()).isEqualTo(ListCourseEnrollmentsQuery.MAX_SIZE);
        assertThat(new ListCourseEnrollmentsQuery(null, 0, Integer.MAX_VALUE).size()).isEqualTo(ListCourseEnrollmentsQuery.MAX_SIZE);
        assertThat(new ListCourseEnrollmentsQuery(null, 0, ListCourseEnrollmentsQuery.MAX_SIZE).size()).isEqualTo(ListCourseEnrollmentsQuery.MAX_SIZE);
    }
}
