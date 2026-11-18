package com.educational.platform.course.reviews;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.reviews.reviewer.Reviewer;
import com.educational.platform.course.reviews.reviewer.ReviewerRepository;

/**
 * Represents the logic for retrieving the reviewer entity from database for current authenticated user.
 */
@Component
public class CurrentUserAsReviewer {

    private final ReviewerRepository reviewerRepository;

    public CurrentUserAsReviewer(ReviewerRepository reviewerRepository) {
        this.reviewerRepository = reviewerRepository;
    }

    /**
     * Represents current user as reviewer.
     *
     * @return reviewer.
     * @throws RelatedResourceIsNotResolvedException if reviewer is not found for the current user
     */
    public Reviewer userAsReviewer() {
        var principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var username = principal.getUsername();

        return reviewerRepository.findByUsername(username)
                .orElseThrow(() -> new RelatedResourceIsNotResolvedException("Reviewer cannot be found by username = " + username));
    }

}
