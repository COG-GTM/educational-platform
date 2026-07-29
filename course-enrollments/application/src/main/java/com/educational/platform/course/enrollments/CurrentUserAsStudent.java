package com.educational.platform.course.enrollments;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;

/**
 * Represents the logic for retrieving the student entity from database for current authenticated user.
 *
 * Lookup is intentionally keyed on username: the security principal only exposes the username of the
 * logged-in user. This is a login/security concern within this module, not a cross-module identity
 * handle — cross-module references use the student's stable UUID ({@link Student#toReference()}).
 */
@Component
public class CurrentUserAsStudent {

    private final StudentRepository studentRepository;

    public CurrentUserAsStudent(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Represents current user as student.
     *
     * @return teacher.
     */
    public Student userAsStudent() {
        var principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var username = principal.getUsername();

        return studentRepository.findByUsername(username);
    }

}
