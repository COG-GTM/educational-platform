package com.educational.platform.course.enrollments.student;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Represents student domain model.
 */
@Entity
@Table(indexes = @Index(name = "student_username_idx", columnList = "username"))
public class Student implements AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;

    // for JPA
    private Student() {
    }

    public Student(CreateStudentCommand createStudentCommand) {
        this.username = createStudentCommand.username();
    }

    public Integer getId() {
        return id;
    }

    public String toReference() {
        return username;
    }
}
