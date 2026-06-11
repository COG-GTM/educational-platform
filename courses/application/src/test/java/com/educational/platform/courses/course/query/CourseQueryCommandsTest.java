package com.educational.platform.courses.course.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseQueryCommandsTest {

    @Test
    void listCourseQuery_canBeInstantiated() {
        final ListCourseQuery query = new ListCourseQuery();
        assertThat(query).isNotNull();
    }

    @Test
    void listCourseQuery_twoInstancesAreNotSameReference() {
        assertThat(new ListCourseQuery()).isNotSameAs(new ListCourseQuery());
    }
}
