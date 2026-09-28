package com.educational.platform.course.reviews;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents Course Review domain model.
 */
@Entity
@Table(name = "course_review", uniqueConstraints = @UniqueConstraint(name = "course_review_reviewer_course_uk", columnNames = {"reviewer", "course"}))
public class CourseReview implements AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private UUID uuid;

    private Integer reviewer;
    private Integer course;
    private CourseRating rating;
    private Comment comment;

    // for JPA
    private CourseReview() {

    }

    CourseReview(ReviewCourseCommand command, Integer course, Integer reviewer) {
        this.uuid = UUID.randomUUID();
        this.course = course;
        this.reviewer = reviewer;
        this.rating = new CourseRating(command.rating());
        this.comment = new Comment(command.comment());
    }

    public void update(UpdateCourseReviewCommand command) {
        this.rating = new CourseRating(command.rating());
        this.comment = new Comment(command.comment());
    }

    public UUID toIdentifier() {
        return this.uuid;
    }
}
