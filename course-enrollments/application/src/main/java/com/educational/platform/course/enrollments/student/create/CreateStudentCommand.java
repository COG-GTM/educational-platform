package com.educational.platform.course.enrollments.student.create;

import java.util.UUID;

/**
 * Represents Create student command.
 */
public record CreateStudentCommand(UUID uuid, String username) {

}
