package com.educational.platform.course.reviews.enrollment.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Represents create reviewer enrollment command.
 */
public record CreateReviewerEnrollmentCommand(@NotNull UUID courseId, @NotBlank String username) {

}
