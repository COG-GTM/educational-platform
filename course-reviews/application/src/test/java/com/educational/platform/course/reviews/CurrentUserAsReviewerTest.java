package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsReviewerTest {

    @Mock
    private ReviewerRepository reviewerRepository;

    @InjectMocks
    private CurrentUserAsReviewer sut;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsReviewer_authenticatedUser_resolvesPrincipalUsernameAndReturnsRepositoryReviewer() {
        // given - the review flow resolves the acting reviewer from the authenticated principal's username
        // before attaching it to the created course review
        authenticateAs("reviewer");
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer"));
        when(reviewerRepository.findByUsername("reviewer")).thenReturn(reviewer);

        // when
        final Reviewer result = sut.userAsReviewer();

        // then
        assertThat(result).isSameAs(reviewer);
        verify(reviewerRepository).findByUsername("reviewer");
    }

    @Test
    void userAsReviewer_noMatchingReviewer_returnsNull() {
        // given - the resolver performs no guard, so a username with no reviewer projection is forwarded as null
        authenticateAs("reviewer");
        when(reviewerRepository.findByUsername("reviewer")).thenReturn(null);

        // when
        final Reviewer result = sut.userAsReviewer();

        // then
        assertThat(result).isNull();
    }

    private void authenticateAs(String username) {
        final UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("password")
                .authorities("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
    }
}
