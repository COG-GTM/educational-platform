package com.educational.platform.course.enrollments.student;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Represents student repository.
 */
public interface StudentRepository extends JpaRepository<Student, Integer> {

    /**
     * Retrieves a student by its username.
     *
     * @param username must not be {@literal null}.
     * @return the student with the given username.
     * @throws IllegalArgumentException if {@literal username} is {@literal null}.
     */
    Student findByUsername(String username);

    /**
     * Checks whether a student with the given username exists.
     *
     * @param username must not be {@literal null}.
     * @return {@literal true} if a student with the given username exists.
     */
    boolean existsByUsername(String username);

}
