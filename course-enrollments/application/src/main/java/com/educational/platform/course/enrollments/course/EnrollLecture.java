package com.educational.platform.course.enrollments.course;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

/**
 * Represents a lecture of an enrollable course. Students track their progress against lectures.
 */
@Entity(name = "enroll_lecture")
public class EnrollLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private UUID uuid;

    private String title;

    private Integer serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course")
    private EnrollCourse course;

    // for JPA
    protected EnrollLecture() {
    }

    EnrollLecture(CreateCourseCommand.CreateLectureCommand command, EnrollCourse course) {
        this.uuid = command.uuid();
        this.title = command.title();
        this.serialNumber = command.serialNumber();
        this.course = course;
    }

    void update(CreateCourseCommand.CreateLectureCommand command) {
        this.title = command.title();
        this.serialNumber = command.serialNumber();
    }

    public Integer getId() {
        return id;
    }

    public UUID toReference() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public Integer getSerialNumber() {
        return serialNumber;
    }
}
