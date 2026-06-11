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

    @Test
    void hasAccess_repositoryThrows_exceptionPropagates() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(authentication.getName()).thenReturn("user");
        when(courseReviewRepository.isReviewer(reviewId, "user"))
                .thenThrow(new RuntimeException("db error"));

        // when/then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sut.hasAccess(authentication, reviewId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    void hasAccess_differentReviewIds_delegatesCorrectIdToRepository() {
        // given
        final UUID reviewId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID reviewId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        when(authentication.getName()).thenReturn("user");
        when(courseReviewRepository.isReviewer(reviewId1, "user")).thenReturn(true);
        when(courseReviewRepository.isReviewer(reviewId2, "user")).thenReturn(false);

        // when
        final boolean result1 = sut.hasAccess(authentication, reviewId1);
        final boolean result2 = sut.hasAccess(authentication, reviewId2);

        // then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
        verify(courseReviewRepository).isReviewer(reviewId1, "user");
        verify(courseReviewRepository).isReviewer(reviewId2, "user");
    }

    @Test
    void hasAccess_nullAuthentication_throwsNullPointerException() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when/then
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> sut.hasAccess(null, reviewId));
    }

    @Test
    void hasAccess_authenticationNameReturnsNull_delegatesToRepositoryWithNull() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(authentication.getName()).thenReturn(null);
        when(courseReviewRepository.isReviewer(reviewId, null)).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isFalse();
        verify(courseReviewRepository).isReviewer(reviewId, null);
    }

    @Test
    void hasAccess_calledExactlyOnce_singleRepositoryInvocation() {
        // given
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(authentication.getName()).thenReturn("user");
        when(courseReviewRepository.isReviewer(reviewId, "user")).thenReturn(true);

        // when
        sut.hasAccess(authentication, reviewId);

        // then
        org.mockito.Mockito.verify(courseReviewRepository, org.mockito.Mockito.times(1)).isReviewer(reviewId, "user");
    }

    @Test
    void hasAccess_calledTwiceWithSameArgs_doesNotCache() {
        // given — checker must not cache; each call must delegate
        final UUID reviewId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        when(authentication.getName()).thenReturn("user");
        when(courseReviewRepository.isReviewer(reviewId, "user")).thenReturn(true);

        // when
        sut.hasAccess(authentication, reviewId);
        sut.hasAccess(authentication, reviewId);

        // then
        org.mockito.Mockito.verify(courseReviewRepository, org.mockito.Mockito.times(2)).isReviewer(reviewId, "user");
    }
}
