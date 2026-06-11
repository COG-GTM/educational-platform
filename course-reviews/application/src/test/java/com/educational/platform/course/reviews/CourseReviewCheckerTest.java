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
        when(authentication.getName()).thenReturn("username");
        when(courseReviewRepository.isReviewer(reviewId, "username")).thenReturn(true);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccess_userIsNotReviewer_returnsFalse() {
        // given
        final UUID reviewId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("username");
        when(courseReviewRepository.isReviewer(reviewId, "username")).thenReturn(false);

        // when
        final boolean result = sut.hasAccess(authentication, reviewId);

        // then
        assertThat(result).isFalse();
    }
}
