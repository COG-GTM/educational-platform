package com.educational.platform.course.enrollments;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the student's enrollment with its learning progress.
 */
public record CourseEnrollmentDTO(UUID uuid, UUID course, String courseName, String student,
                                  CompletionStatusDTO completionStatus, boolean archived,
                                  int completedLectures, int totalLectures, int progressPercent,
                                  LocalDateTime enrolledAt, LocalDateTime lastActivityAt, LocalDateTime completedAt) {

}
