package com.educational.platform.course.enrollments.course;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

/**
 * Represents course domain model.
 */
@Entity(name = "enroll_course")
public class EnrollCourse implements AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private UUID uuid;

    private String name;

    @BatchSize(size = 50)
    @OrderBy("serialNumber ASC, id ASC")
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnrollLecture> lectures = new ArrayList<>();

    // for JPA
    protected EnrollCourse() {

    }

    public EnrollCourse(CreateCourseCommand createCourseCommand) {
        this.uuid = createCourseCommand.uuid();
        refresh(createCourseCommand);
    }

    /**
     * Applies the latest published snapshot of the course. Lectures already known (same uuid) keep their identity so
     * the progress of enrolled students is preserved.
     *
     * @param command published course snapshot.
     * @return this course.
     */
    public EnrollCourse refresh(CreateCourseCommand command) {
        this.name = command.name();
        final List<CreateCourseCommand.CreateLectureCommand> published = command.lectures() == null ? List.of() : command.lectures();
        lectures.removeIf(lecture -> published.stream().noneMatch(p -> p.uuid().equals(lecture.toReference())));
        for (CreateCourseCommand.CreateLectureCommand lecture : published) {
            lectures.stream()
                    .filter(existing -> existing.toReference().equals(lecture.uuid()))
                    .findFirst()
                    .ifPresentOrElse(existing -> existing.update(lecture), () -> lectures.add(new EnrollLecture(lecture, this)));
        }
        return this;
    }

    public Integer getId() {
        return id;
    }

    public UUID toReference() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public List<EnrollLecture> getLectures() {
        return Collections.unmodifiableList(lectures);
    }

    public Optional<EnrollLecture> lectureByUuid(UUID lectureUuid) {
        return lectures.stream().filter(lecture -> lecture.toReference().equals(lectureUuid)).findFirst();
    }
}
