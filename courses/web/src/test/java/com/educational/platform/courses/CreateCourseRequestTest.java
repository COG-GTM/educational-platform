package com.educational.platform.courses;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCourseRequestTest {

    @Test
    void constructor_validArguments_requestCreated() {
        // when
        final CreateCourseRequest sut = new CreateCourseRequest("Java Basics", "Learn Java");

        // then
        assertThat(sut.name()).isEqualTo("Java Basics");
        assertThat(sut.description()).isEqualTo("Learn Java");
    }

    @Test
    void equals_sameValues_equal() {
        // given
        final CreateCourseRequest request1 = new CreateCourseRequest("name", "desc");
        final CreateCourseRequest request2 = new CreateCourseRequest("name", "desc");

        // when / then
        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void equals_differentValues_notEqual() {
        // given
        final CreateCourseRequest request1 = new CreateCourseRequest("name1", "desc1");
        final CreateCourseRequest request2 = new CreateCourseRequest("name2", "desc2");

        // when / then
        assertThat(request1).isNotEqualTo(request2);
    }
}
