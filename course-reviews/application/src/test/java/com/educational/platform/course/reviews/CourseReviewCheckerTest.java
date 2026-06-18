package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseReviewCheckerTest {

    private static final UUID REVIEW_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CourseReviewChecker sut;

    @Test
    void hasAccess_currentUserIsReviewer_accessGranted() {
        // given - access is delegated to the repository, which checks ownership by the authenticated username
        when(authentication.getName()).thenReturn("reviewer");
        when(courseReviewRepository.isReviewer(REVIEW_ID, "reviewer")).thenReturn(true);

        // when
        final boolean access = sut.hasAccess(authentication, REVIEW_ID);

        // then
        assertThat(access).isTrue();
    }

    @Test
    void hasAccess_currentUserIsNotReviewer_accessDenied() {
        // given - a user who does not own the review is denied
        when(authentication.getName()).thenReturn("someoneelse");
        when(courseReviewRepository.isReviewer(REVIEW_ID, "someoneelse")).thenReturn(false);

        // when
        final boolean access = sut.hasAccess(authentication, REVIEW_ID);

        // then
        assertThat(access).isFalse();
    }

    @Test
    void hasAccess_delegatesReviewIdAndAuthenticatedUsernameToRepository() {
        // given - the checker must forward the review uuid and the authenticated principal's name verbatim
        when(authentication.getName()).thenReturn("reviewer");
        when(courseReviewRepository.isReviewer(REVIEW_ID, "reviewer")).thenReturn(true);

        // when
        sut.hasAccess(authentication, REVIEW_ID);

        // then
        verify(courseReviewRepository).isReviewer(REVIEW_ID, "reviewer");
    }
}
