package com.educational.platform.course.reviews;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;
import com.educational.platform.course.reviews.reviewer.create.CreateReviewerCommand;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrentUserAsReviewerTest {

    @Mock
    private ReviewerRepository reviewerRepository;

    @InjectMocks
    private CurrentUserAsReviewer sut;

    @BeforeEach
    void setUp() {
        final UserDetails principal = new User("username", "password", List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userAsReviewer_reviewerExists_reviewerReturned() {
        // given
        final Reviewer reviewer = new Reviewer(new CreateReviewerCommand("username"));
        when(reviewerRepository.findByUsername("username")).thenReturn(Optional.of(reviewer));

        // when
        final Reviewer result = sut.userAsReviewer();

        // then
        assertThat(result).isSameAs(reviewer);
    }

    @Test
    void userAsReviewer_reviewerNotFound_relatedResourceIsNotResolvedException() {
        // given
        when(reviewerRepository.findByUsername("username")).thenReturn(Optional.empty());

        // when
        final ThrowingCallable action = () -> sut.userAsReviewer();

        // then
        assertThatThrownBy(action)
                .isInstanceOf(RelatedResourceIsNotResolvedException.class)
                .hasMessageContaining("username");
    }

}
