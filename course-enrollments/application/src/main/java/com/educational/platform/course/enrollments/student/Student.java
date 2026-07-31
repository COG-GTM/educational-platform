package com.educational.platform.course.enrollments.student;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.UUID;

/**
 * Represents student domain model.
 */
@Entity
public class Student implements AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private UUID uuid;

    // kept for security-context lookups: the authenticated principal only exposes the username
    private String username;

    // for JPA
    private Student() {
    }

    public Student(CreateStudentCommand createStudentCommand) {
        this.uuid = createStudentCommand.uuid();
        this.username = createStudentCommand.username();
    }

    public Integer getId() {
        return id;
    }

    /**
     * Assigns the stable user uuid, reconciling students that were created before
     * the user uuid was known (e.g. legacy rows with a generated uuid).
     *
     * @param uuid stable user uuid
     */
    public void assignUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID toReference() {
        return uuid;
    }
}
