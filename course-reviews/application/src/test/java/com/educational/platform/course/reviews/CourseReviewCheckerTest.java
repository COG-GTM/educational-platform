package com.educational.platform.course.reviews;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseReviewCheckerTest {

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private Authentication authentication;

    private CourseReviewChecker sut;

    @BeforeEach
    void setUp() {
        sut = new CourseReviewChecker(courseReviewRepository);
    }

    @Test
    void hasAccess_reviewerIsOwner_returnsTrue() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(authentication.getName()).thenReturn("reviewer");
        when(courseReviewRepository.isReviewer(reviewId, "reviewer")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_reviewerIsNotOwner_returnsFalse() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(authentication.getName()).thenReturn("other-user");
        when(courseReviewRepository.isReviewer(reviewId, "other-user")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void hasAccess_delegatesToRepositoryWithCorrectArguments() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(authentication.getName()).thenReturn("test-user");
        when(courseReviewRepository.isReviewer(reviewId, "test-user")).thenReturn(true);

        // when
        sut.hasAccess(authentication, reviewId);

        // then
        verify(courseReviewRepository).isReviewer(reviewId, "test-user");
    }

    @Test
    void hasAccess_nullReviewId_delegatesToRepository() {
        // given
        when(authentication.getName()).thenReturn("user");
        when(courseReviewRepository.isReviewer(null, "user")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, null);

        // then
        assertThat(result).isFalse();
        verify(courseReviewRepository).isReviewer(null, "user");
    }
}
