package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollmentCommandsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void registerStudentToCourseCommand_exposesCourseId() {
        assertThat(new RegisterStudentToCourseCommand(UUID_VALUE).courseId()).isEqualTo(UUID_VALUE);
    }

    @Test
    void courseEnrollmentByUUIDQuery_exposesUuid() {
        assertThat(new CourseEnrollmentByUUIDQuery(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void listCourseEnrollmentsQuery_equalInstances() {
        assertThat(new ListCourseEnrollmentsQuery()).isEqualTo(new ListCourseEnrollmentsQuery());
    }
}
