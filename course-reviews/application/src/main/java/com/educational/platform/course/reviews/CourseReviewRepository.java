package com.educational.platform.course.reviews;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Represents course review repository.
 */
public interface CourseReviewRepository extends JpaRepository<CourseReview, Integer> {

    /**
     * Retrieves a course review by its uuid.
     *
     * @param uuid must not be {@literal null}.
     * @return the course review with the given uuid or {@literal Optional#empty()} if none found.
     * @throws IllegalArgumentException if {@literal uuid} is {@literal null}.
     */
    Optional<CourseReview> findByUuid(UUID uuid);

    /**
     * Retrieves course reviews of a course, most recent first.
     *
     * @param uuid course uuid.
     * @return list of course reviews.
     */
    @Query("select new com.educational.platform.course.reviews.CourseReviewDTO(cr.uuid, c.originalCourseId, r.username, cr.comment.comment, cr.rating.rating, cr.createdDate) from CourseReview cr "
            + "join com.educational.platform.course.reviews.course.ReviewableCourse c on cr.course = c.id "
            + "join com.educational.platform.course.reviews.reviewer.Reviewer r on cr.reviewer = r.id "
            + "where c.originalCourseId = :uuid "
            + "order by cr.createdDate desc, cr.id desc")
    List<CourseReviewDTO> listCourseReviews(@Param("uuid") UUID uuid);

    /**
     * Retrieves all ratings given to a course.
     *
     * @param uuid course uuid.
     * @return list of ratings.
     */
    @Query("select cr.rating.rating from CourseReview cr "
            + "join com.educational.platform.course.reviews.course.ReviewableCourse c on cr.course = c.id "
            + "where c.originalCourseId = :uuid")
    List<Double> listRatings(@Param("uuid") UUID uuid);

    /**
     * Checks if passed username is an username of reviewer of course review.
     *
     * @param uuid course review uuid.
     * @param username username.
     * @return true if reviewer, false if not.
     */
    @Query("select count(cr) > 0 from CourseReview cr join com.educational.platform.course.reviews.reviewer.Reviewer r on cr.reviewer = r.id where cr.uuid = :uuid and r.username = :username")
    boolean isReviewer(@Param("uuid") UUID uuid, @Param("username") String username);
}
