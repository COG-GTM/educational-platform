package com.educational.platform.course.reviews;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void hasAccess_userIsReviewer_returnsTrue() {
        // given
        final UUID reviewId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("reviewer");
        when(courseReviewRepository.isReviewer(reviewId, "reviewer")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_userIsNotReviewer_returnsFalse() {
        // given
        final UUID reviewId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("other-user");
        when(courseReviewRepository.isReviewer(reviewId, "other-user")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void hasAccess_differentReviewIds_queriesCorrectId() {
        // given
        final UUID reviewId1 = UUID.randomUUID();
        final UUID reviewId2 = UUID.randomUUID();
        when(authentication.getName()).thenReturn("user");
        when(courseReviewRepository.isReviewer(reviewId1, "user")).thenReturn(true);
        when(courseReviewRepository.isReviewer(reviewId2, "user")).thenReturn(false);

        // when / then
        assertThat(sut.hasAccess(authentication, reviewId1)).isTrue();
        assertThat(sut.hasAccess(authentication, reviewId2)).isFalse();
    }
}
