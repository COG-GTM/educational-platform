package com.educational.platform.course.enrollments.register;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterStudentToCourseCommandTest {

    @Test
    void constructor_validCourseId_courseIdAccessible() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final RegisterStudentToCourseCommand sut = new RegisterStudentToCourseCommand(courseId);

        // then
        assertThat(sut.courseId()).isEqualTo(courseId);
    }

    @Test
    void constructor_nullCourseId_courseIdIsNull() {
        // when
        final RegisterStudentToCourseCommand sut = new RegisterStudentToCourseCommand(null);

        // then
        assertThat(sut.courseId()).isNull();
    }

    @Test
    void equality_sameCourseId_equal() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand cmd1 = new RegisterStudentToCourseCommand(courseId);
        final RegisterStudentToCourseCommand cmd2 = new RegisterStudentToCourseCommand(courseId);

        // then
        assertThat(cmd1).isEqualTo(cmd2);
        assertThat(cmd1.hashCode()).isEqualTo(cmd2.hashCode());
    }

    @Test
    void equality_differentCourseId_notEqual() {
        // given
        final RegisterStudentToCourseCommand cmd1 = new RegisterStudentToCourseCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        final RegisterStudentToCourseCommand cmd2 = new RegisterStudentToCourseCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // then
        assertThat(cmd1).isNotEqualTo(cmd2);
    }
}
