package com.educational.platform.course.enrollments;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;

/**
 * Represents the logic for retrieving the student entity from database for current authenticated user.
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
        return studentRepository.findByUsername(username());
    }

    /**
     * Represents the username of the current user.
     *
     * @return username.
     */
    public String username() {
        var principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getUsername();
    }

}
