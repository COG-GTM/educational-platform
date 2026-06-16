package com.educational.platform.courses.teacher;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Represents Teacher model.
 */
@Entity
public class Teacher implements AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    private String username;

    // for JPA
    private Teacher() {

    }

    public Teacher(CreateTeacherCommand command) {
        this.username = command.username();
    }

    public Integer getId() {
        return id;
    }

    public String toIdentity() {
        return username;
    }
}
