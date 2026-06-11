package com.educational.platform.courses.course.query;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseQueryRecordsTest {

    @Test
    void courseByUUIDQuery_exposesUuid() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        final CourseByUUIDQuery query = new CourseByUUIDQuery(uuid);

        // then
        assertThat(query.uuid()).isEqualTo(uuid);
    }

    @Test
    void courseByUUIDQuery_equalInstances() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        assertThat(new CourseByUUIDQuery(uuid)).isEqualTo(new CourseByUUIDQuery(uuid));
    }

    @Test
    void courseByUUIDQuery_differentUuids_notEqual() {
        assertThat(new CourseByUUIDQuery(UUID.randomUUID()))
                .isNotEqualTo(new CourseByUUIDQuery(UUID.randomUUID()));
    }

    @Test
    void listCourseQuery_canBeInstantiated() {
        assertThat(new ListCourseQuery()).isNotNull();
    }
}
