package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentByCourseQueryHandlerTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Mock
    private CourseEnrollmentRepository repository;

    @Mock
    private EnrollCourseRepository courseRepository;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    private EnrollCourse course;
    private Student student;

    private CourseEnrollmentByCourseQueryHandler sut;

    @BeforeEach
    void setUp() {
        course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", java.util.List.of()));
        student = new Student(new CreateStudentCommand("username"));
        sut = new CourseEnrollmentByCourseQueryHandler(repository, courseRepository, currentUserAsStudent);
    }

    @Test
    void handle_studentEnrolled_enrollmentDTO() {
        // given
        final CourseEnrollment enrollment = new CourseEnrollment(course, student);
        when(courseRepository.findByUuid(COURSE)).thenReturn(Optional.of(course));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByCourseAndStudent(course, student)).thenReturn(Optional.of(enrollment));

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(new CourseEnrollmentByCourseQuery(COURSE));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().uuid()).isEqualTo(enrollment.getUuid());
        assertThat(result.get().course()).isEqualTo(COURSE);
        assertThat(result.get().student()).isEqualTo("username");
    }

    @Test
    void handle_studentNotEnrolled_empty() {
        // given
        when(courseRepository.findByUuid(COURSE)).thenReturn(Optional.of(course));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByCourseAndStudent(course, student)).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(new CourseEnrollmentByCourseQuery(COURSE));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_unknownCourse_emptyWithoutLookingUpEnrollments() {
        // given
        when(courseRepository.findByUuid(COURSE)).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDTO> result = sut.handle(new CourseEnrollmentByCourseQuery(COURSE));

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(repository, currentUserAsStudent);
    }
}
