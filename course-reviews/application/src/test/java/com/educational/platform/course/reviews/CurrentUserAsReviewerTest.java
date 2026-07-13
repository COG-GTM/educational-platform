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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsReviewerTest {

    @Mock
    private ReviewerRepository reviewerRepository;

    @InjectMocks
    private CurrentUserAsReviewer sut;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsReviewer_authenticatedUser_reviewerRetrievedByUsername() {
        // given
        final UserDetails principal = new User("reviewer-username", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "password", Collections.emptyList()));

        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("reviewer-username"));
        when(reviewerRepository.findByUsername("reviewer-username")).thenReturn(reviewer);

        // when
        final Reviewer result = sut.userAsReviewer();

        // then
        assertThat(result).isEqualTo(reviewer);
    }
}
