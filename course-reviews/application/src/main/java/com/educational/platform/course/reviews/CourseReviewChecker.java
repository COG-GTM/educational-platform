package com.educational.platform.course.reviews;

import java.util.UUID;

import com.educational.platform.course.reviews.enrollment.ReviewerEnrollmentRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Represents the logic for checking if user is a reviewer of course review or is allowed to review a course
 */
@Component
public class CourseReviewChecker {

	private final CourseReviewRepository courseReviewRepository;
	private final ReviewerEnrollmentRepository reviewerEnrollmentRepository;

    public CourseReviewChecker(CourseReviewRepository courseReviewRepository, ReviewerEnrollmentRepository reviewerEnrollmentRepository) {
        this.courseReviewRepository = courseReviewRepository;
        this.reviewerEnrollmentRepository = reviewerEnrollmentRepository;
    }

    public boolean hasAccess(Authentication authentication, UUID reviewId) {
		return courseReviewRepository.isReviewer(reviewId, authentication.getName());
	}

    public boolean isEnrolled(Authentication authentication, UUID courseId) {
        return courseId != null && reviewerEnrollmentRepository.existsByCourseIdAndUsername(courseId, authentication.getName());
    }

}
