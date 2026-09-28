package com.educational.platform.course.reviews.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Represents reviewer enrollment repository.
 */
public interface ReviewerEnrollmentRepository extends JpaRepository<ReviewerEnrollment, Integer> {

    /**
     * Checks if the reviewer with passed username is enrolled to the course.
     *
     * @param courseId original course uuid.
     * @param username reviewer username.
     * @return true if enrolled, false if not.
     */
    boolean existsByCourseIdAndUsername(UUID courseId, String username);

}
