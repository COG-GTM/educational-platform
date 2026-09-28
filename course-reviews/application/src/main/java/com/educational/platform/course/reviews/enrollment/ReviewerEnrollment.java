package com.educational.platform.course.reviews.enrollment;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.course.reviews.enrollment.create.CreateReviewerEnrollmentCommand;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * Represents Reviewer Enrollment domain model, a local projection of the student enrollment to course.
 */
@Entity
@Table(name = "reviewer_enrollment", uniqueConstraints = @UniqueConstraint(name = "reviewer_enrollment_course_id_username_uk", columnNames = {"course_id", "username"}))
public class ReviewerEnrollment implements AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private UUID courseId;

    private String username;

    // for JPA
    private ReviewerEnrollment() {
    }

    public ReviewerEnrollment(CreateReviewerEnrollmentCommand command) {
        this.courseId = command.courseId();
        this.username = command.username();
    }

    public Integer getId() {
        return id;
    }
}
