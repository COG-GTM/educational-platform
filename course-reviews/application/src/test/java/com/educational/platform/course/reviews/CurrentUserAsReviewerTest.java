package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsReviewerTest {

    @Mock
    private ReviewerRepository reviewerRepository;

    private CurrentUserAsReviewer sut;

    @BeforeEach
    void setUp() {
        sut = new CurrentUserAsReviewer(reviewerRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsReviewer_authenticatedUser_reviewerReturned() {
        // given
        final var userDetails = new User("reviewer-user", "password", Collections.emptyList());
        final var authentication = new UsernamePasswordAuthenticationToken(userDetails, "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final Reviewer expectedReviewer = new Reviewer(new CreateReviewerCommand("reviewer-user"));
        when(reviewerRepository.findByUsername("reviewer-user")).thenReturn(expectedReviewer);

        // when
        final Reviewer result = sut.userAsReviewer();

        // then
        assertThat(result).isSameAs(expectedReviewer);
        verify(reviewerRepository).findByUsername("reviewer-user");
    }

    @Test
    void userAsReviewer_differentUser_delegatesToRepositoryWithCorrectUsername() {
        // given
        final var userDetails = new User("another-user", "password", Collections.emptyList());
        final var authentication = new UsernamePasswordAuthenticationToken(userDetails, "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final Reviewer expectedReviewer = new Reviewer(new CreateReviewerCommand("another-user"));
        when(reviewerRepository.findByUsername("another-user")).thenReturn(expectedReviewer);

        // when
        final Reviewer result = sut.userAsReviewer();

        // then
        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("username", "another-user");
    }

    @Test
    void userAsReviewer_repositoryReturnsNull_nullReturned() {
        // given
        final var userDetails = new User("unknown-user", "password", Collections.emptyList());
        final var authentication = new UsernamePasswordAuthenticationToken(userDetails, "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(reviewerRepository.findByUsername("unknown-user")).thenReturn(null);

        // when
        final Reviewer result = sut.userAsReviewer();

        // then
        assertThat(result).isNull();
    }

    @Test
    void userAsReviewer_authenticatedUser_delegatesToRepositoryWithCorrectUsername() {
        // given
        final var userDetails = new User("verify-user", "password", Collections.emptyList());
        final var authentication = new UsernamePasswordAuthenticationToken(userDetails, "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(reviewerRepository.findByUsername("verify-user")).thenReturn(null);

        // when
        sut.userAsReviewer();

        // then
        verify(reviewerRepository).findByUsername("verify-user");
    }

    @Test
    void userAsReviewer_noAuthenticationInContext_throwsNullPointerException() {
        // given
        SecurityContextHolder.clearContext();

        // when/then
        assertThatThrownBy(() -> sut.userAsReviewer())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void userAsReviewer_calledTwice_delegatesToRepositoryEachTime() {
        // given
        final var userDetails = new User("repeat-user", "password", Collections.emptyList());
        final var authentication = new UsernamePasswordAuthenticationToken(userDetails, "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final Reviewer expectedReviewer = new Reviewer(new CreateReviewerCommand("repeat-user"));
        when(reviewerRepository.findByUsername("repeat-user")).thenReturn(expectedReviewer);

        // when
        sut.userAsReviewer();
        sut.userAsReviewer();

        // then
        verify(reviewerRepository, org.mockito.Mockito.times(2)).findByUsername("repeat-user");
    }

    @Test
    void userAsReviewer_userChangedBetweenCalls_usesLatestSecurityContext() {
        // given — first call as "user-a", then security context switches to "user-b"
        final var userDetailsA = new User("user-a", "password", Collections.emptyList());
        final var authA = new UsernamePasswordAuthenticationToken(userDetailsA, "password");
        SecurityContextHolder.getContext().setAuthentication(authA);

        final Reviewer reviewerA = new Reviewer(new CreateReviewerCommand("user-a"));
        when(reviewerRepository.findByUsername("user-a")).thenReturn(reviewerA);

        // when — first call
        final Reviewer resultA = sut.userAsReviewer();

        // switch context to user-b
        final var userDetailsB = new User("user-b", "password", Collections.emptyList());
        final var authB = new UsernamePasswordAuthenticationToken(userDetailsB, "password");
        SecurityContextHolder.getContext().setAuthentication(authB);

        final Reviewer reviewerB = new Reviewer(new CreateReviewerCommand("user-b"));
        when(reviewerRepository.findByUsername("user-b")).thenReturn(reviewerB);

        // when — second call
        final Reviewer resultB = sut.userAsReviewer();

        // then — each call used the respective current context
        assertThat(resultA).hasFieldOrPropertyWithValue("username", "user-a");
        assertThat(resultB).hasFieldOrPropertyWithValue("username", "user-b");
        verify(reviewerRepository).findByUsername("user-a");
        verify(reviewerRepository).findByUsername("user-b");
    }

    @Test
    void userAsReviewer_repositoryThrows_exceptionPropagates() {
        // given
        final var userDetails = new User("error-user", "password", Collections.emptyList());
        final var authentication = new UsernamePasswordAuthenticationToken(userDetails, "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(reviewerRepository.findByUsername("error-user")).thenThrow(new RuntimeException("db error"));

        // when/then
        assertThatThrownBy(() -> sut.userAsReviewer())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    void userAsReviewer_principalIsNotUserDetails_throwsClassCastException() {
        // given — authentication with a String principal instead of UserDetails
        final var authentication = new UsernamePasswordAuthenticationToken("plain-string-principal", "password");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when/then
        assertThatThrownBy(() -> sut.userAsReviewer())
                .isInstanceOf(ClassCastException.class);
    }
}
