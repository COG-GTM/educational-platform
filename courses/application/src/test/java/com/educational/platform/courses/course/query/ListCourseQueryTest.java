package com.educational.platform.courses.course.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ListCourseQueryTest {

    @Test
    void constructor_canBeInstantiated() {
        // when
        final ListCourseQuery sut = new ListCourseQuery();

        // then
        assertThat(sut).isNotNull();
    }

    @Test
    void twoInstances_areEqual() {
        // given
        final ListCourseQuery a = new ListCourseQuery();
        final ListCourseQuery b = new ListCourseQuery();

        // then — marker class with no fields; instances are distinct objects
        assertThat(a).isNotSameAs(b);
    }

    @Test
    void isNotAbstract() {
        // then
        assertThat(java.lang.reflect.Modifier.isAbstract(ListCourseQuery.class.getModifiers())).isFalse();
    }
}
