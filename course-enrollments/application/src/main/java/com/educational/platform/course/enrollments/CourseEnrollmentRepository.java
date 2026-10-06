package com.educational.platform.course.enrollments;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.student.Student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents course enrollment repository.
 */
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Integer> {

    /**
     * Retrieves a course enrollment by its uuid.
     *
     * @param uuid must not be {@literal null}.
     * @return the course enrollment with the given uuid or {@literal Optional#empty()} if none found.
     * @throws IllegalArgumentException if {@literal uuid} is {@literal null}.
     */
    Optional<CourseEnrollment> findByUuid(UUID uuid);

    /**
     * Retrieves the enrollment of the student by the enrollment uuid.
     *
     * @param uuid    enrollment uuid.
     * @param student student.
     * @return the enrollment or {@literal Optional#empty()} if the student has no such enrollment.
     */
    @EntityGraph(attributePaths = "course")
    Optional<CourseEnrollment> findByUuidAndStudent(UUID uuid, Student student);

    /**
     * Retrieves the enrollment of the student to the course.
     *
     * @param course  course.
     * @param student student.
     * @return the enrollment or {@literal Optional#empty()} if the student is not enrolled.
     */
    @EntityGraph(attributePaths = "course")
    Optional<CourseEnrollment> findByCourseAndStudent(EnrollCourse course, Student student);

    @EntityGraph(attributePaths = "course")
    Page<CourseEnrollment> findByStudent(Student student, Pageable pageable);

    @EntityGraph(attributePaths = "course")
    Page<CourseEnrollment> findByStudentAndArchivedFalseAndCompletionStatus(Student student, CompletionStatus completionStatus, Pageable pageable);

    @EntityGraph(attributePaths = "course")
    Page<CourseEnrollment> findByStudentAndArchivedTrue(Student student, Pageable pageable);

    long countByStudentAndArchivedFalseAndCompletionStatus(Student student, CompletionStatus completionStatus);

    long countByStudentAndArchivedTrue(Student student);

}
