package com.educational.platform.course.enrollments.student;

import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Student} construction and accessor methods.
 */
public class StudentConstructionTest {

    @Test
    void constructor_setsUsername() {
        // given
        final Student student = new Student(new CreateStudentCommand("student1"));

        // then
        assertThat(student.toReference()).isEqualTo("student1");
    }

    @Test
    void constructor_initialIdIsNull() {
        // given
        final Student student = new Student(new CreateStudentCommand("student2"));

        // then
        assertThat(student.getId()).isNull();
    }

    @Test
    void toReference_returnsUsername() {
        // given
        final Student student = new Student(new CreateStudentCommand("john_student"));

        // when / then
        assertThat(student.toReference()).isEqualTo("john_student");
    }
}
