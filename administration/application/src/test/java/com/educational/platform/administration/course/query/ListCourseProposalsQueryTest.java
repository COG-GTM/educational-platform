package com.educational.platform.administration.course.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListCourseProposalsQueryTest {

    @Test
    void recordEquality_twoInstances_equal() {
        // when
        final ListCourseProposalsQuery query1 = new ListCourseProposalsQuery();
        final ListCourseProposalsQuery query2 = new ListCourseProposalsQuery();

        // then
        assertThat(query1).isEqualTo(query2);
        assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
    }

    @Test
    void toString_nonNull() {
        // when
        final ListCourseProposalsQuery query = new ListCourseProposalsQuery();

        // then
        assertThat(query.toString()).isNotNull();
    }
}
