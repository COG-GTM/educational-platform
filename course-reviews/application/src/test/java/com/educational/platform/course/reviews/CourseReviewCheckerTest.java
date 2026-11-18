package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.enrollment.ReviewerEnrollmentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewCheckerTest {

    private final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private ReviewerEnrollmentRepository reviewerEnrollmentRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CourseReviewChecker sut;

    @Test
    void isEnrolled_enrolledUser_true() {
        // given
        when(authentication.getName()).thenReturn("username");
        when(reviewerEnrollmentRepository.existsByCourseIdAndUsername(courseId, "username")).thenReturn(true);

        // when
        final boolean result = sut.isEnrolled(authentication, courseId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isEnrolled_notEnrolledUser_false() {
        // given
        when(authentication.getName()).thenReturn("username");
        when(reviewerEnrollmentRepository.existsByCourseIdAndUsername(courseId, "username")).thenReturn(false);

        // when
        final boolean result = sut.isEnrolled(authentication, courseId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isEnrolled_courseIdIsNull_falseWithoutRepositoryCall() {
        // given/when
        final boolean result = sut.isEnrolled(authentication, null);

        // then
        assertThat(result).isFalse();
        verify(reviewerEnrollmentRepository, never()).existsByCourseIdAndUsername(any(), anyString());
    }

    @Test
    void hasAccess_reviewerOfReview_true() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(authentication.getName()).thenReturn("username");
        when(courseReviewRepository.isReviewer(reviewId, "username")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_notReviewerOfReview_false() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(authentication.getName()).thenReturn("another-username");
        when(courseReviewRepository.isReviewer(reviewId, "another-username")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isFalse();
    }

}
