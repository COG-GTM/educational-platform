package com.educational.platform.courses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents Course Create Request.
 */
public record CreateCourseRequest(@NotBlank String name, @NotBlank String description,
                                  @Size(max = 100) String category) {

}
