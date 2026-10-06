package com.educational.platform.course.enrollments;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollLecture;
import com.educational.platform.course.enrollments.student.Student;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

/**
 * Represents course enrollment domain model: the student's membership in a course together with the learning progress.
 */
@Entity
public class CourseEnrollment implements AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course")
    private EnrollCourse course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student")
    private Student student;

    @Enumerated(EnumType.STRING)
    private CompletionStatus completionStatus;

    private boolean archived;

    private LocalDateTime enrolledAt;

    private LocalDateTime lastActivityAt;

    private LocalDateTime completedAt;

    private LocalDateTime archivedAt;

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "course_enrollment_completed_lecture", joinColumns = @JoinColumn(name = "enrollment"))
    @Column(name = "lecture")
    private Set<UUID> completedLectures = new HashSet<>();

    // for JPA
    private CourseEnrollment() {
    }

    public CourseEnrollment(EnrollCourse course, Student student) {
        this.uuid = UUID.randomUUID();
        this.course = course;
        this.student = student;
        this.completionStatus = CompletionStatus.IN_PROGRESS;
        this.archived = false;
        this.enrolledAt = LocalDateTime.now();
        this.lastActivityAt = enrolledAt;
    }

    public void complete() {
        this.completionStatus = CompletionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.lastActivityAt = completedAt;
    }

    /**
     * Marks the lecture as completed. Once every lecture of the course is completed the enrollment becomes completed.
     *
     * @param lectureUuid lecture uuid.
     * @throws CourseEnrollmentArchivedException if the enrollment is archived.
     * @throws ResourceNotFoundException         if the lecture does not belong to the course.
     */
    public void completeLecture(UUID lectureUuid) {
        requireActive();
        completedLectures.add(lecture(lectureUuid).toReference());
        this.lastActivityAt = LocalDateTime.now();

        final List<EnrollLecture> lectures = course.getLectures();
        final boolean allCompleted = !lectures.isEmpty()
                && lectures.stream().allMatch(lecture -> completedLectures.contains(lecture.toReference()));
        if (allCompleted && completionStatus != CompletionStatus.COMPLETED) {
            complete();
        }
    }

    /**
     * Marks the lecture as not completed. A completed enrollment goes back to in progress.
     *
     * @param lectureUuid lecture uuid.
     * @throws CourseEnrollmentArchivedException if the enrollment is archived.
     * @throws ResourceNotFoundException         if the lecture does not belong to the course.
     */
    public void resetLecture(UUID lectureUuid) {
        requireActive();
        final boolean removed = completedLectures.remove(lecture(lectureUuid).toReference());
        if (!removed) {
            return;
        }
        this.lastActivityAt = LocalDateTime.now();
        if (completionStatus == CompletionStatus.COMPLETED) {
            this.completionStatus = CompletionStatus.IN_PROGRESS;
            this.completedAt = null;
        }
    }

    /**
     * Hides the enrollment from the active learning list. Progress is kept.
     */
    public void archive() {
        if (!archived) {
            this.archived = true;
            this.archivedAt = LocalDateTime.now();
            this.lastActivityAt = archivedAt;
        }
    }

    /**
     * Brings an archived enrollment back to the active learning list.
     */
    public void restore() {
        if (archived) {
            this.archived = false;
            this.archivedAt = null;
            this.lastActivityAt = LocalDateTime.now();
        }
    }

    public EnrollCourse getCourse() {
        return course;
    }

    public Student getStudent() {
        return student;
    }

    public UUID getUuid() {
        return uuid;
    }

    public CourseEnrollmentDTO toDTO() {
        final int total = course.getLectures().size();
        final int completed = (int) course.getLectures().stream()
                .filter(lecture -> completedLectures.contains(lecture.toReference()))
                .count();
        final int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);

        return new CourseEnrollmentDTO(uuid, course.toReference(), course.getName(), student.toReference(),
                completionStatus.toDTO(), archived, completed, total, percent, enrolledAt, lastActivityAt, completedAt);
    }

    public CourseEnrollmentDetailsDTO toDetailsDTO() {
        final List<LectureProgressDTO> lectures = course.getLectures().stream()
                .map(lecture -> new LectureProgressDTO(lecture.toReference(), lecture.getTitle(), lecture.getSerialNumber(),
                        completedLectures.contains(lecture.toReference())))
                .toList();

        return new CourseEnrollmentDetailsDTO(toDTO(), lectures);
    }

    private void requireActive() {
        if (archived) {
            throw new CourseEnrollmentArchivedException(uuid);
        }
    }

    private EnrollLecture lecture(UUID lectureUuid) {
        return course.lectureByUuid(lectureUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Lecture with uuid: %s not found in the course", lectureUuid)));
    }
}
