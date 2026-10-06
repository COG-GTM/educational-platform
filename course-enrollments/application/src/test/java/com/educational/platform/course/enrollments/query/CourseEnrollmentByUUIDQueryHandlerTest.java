package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentDetailsDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.LectureProgressDTO;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentByUUIDQueryHandlerTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
    private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

    @Mock
    private CourseEnrollmentRepository repository;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    private Student student;
    private CourseEnrollment enrollment;

    private CourseEnrollmentByUUIDQueryHandler sut;

    @BeforeEach
    void setUp() {
        final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Intro", 1),
                new CreateCourseCommand.CreateLectureCommand(SECOND_LECTURE, "Variables", 2))));
        student = new Student(new CreateStudentCommand("username"));
        enrollment = new CourseEnrollment(course, student);
        sut = new CourseEnrollmentByUUIDQueryHandler(repository, currentUserAsStudent);
    }

    @Test
    void handle_ownEnrollment_detailsWithCurriculumProgress() {
        // given
        enrollment.completeLecture(FIRST_LECTURE);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByUuidAndStudent(enrollment.getUuid(), student)).thenReturn(Optional.of(enrollment));

        // when
        final Optional<CourseEnrollmentDetailsDTO> result = sut.handle(new CourseEnrollmentByUUIDQuery(enrollment.getUuid()));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().enrollment().uuid()).isEqualTo(enrollment.getUuid());
        assertThat(result.get().enrollment().course()).isEqualTo(COURSE);
        assertThat(result.get().enrollment().student()).isEqualTo("username");
        assertThat(result.get().enrollment().completedLectures()).isEqualTo(1);
        assertThat(result.get().enrollment().totalLectures()).isEqualTo(2);
        assertThat(result.get().lectures()).containsExactly(
                new LectureProgressDTO(FIRST_LECTURE, "Intro", 1, true),
                new LectureProgressDTO(SECOND_LECTURE, "Variables", 2, false));
    }

    @Test
    void handle_enrollmentNotOwnedOrUnknown_empty() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(repository.findByUuidAndStudent(uuid, student)).thenReturn(Optional.empty());

        // when
        final Optional<CourseEnrollmentDetailsDTO> result = sut.handle(new CourseEnrollmentByUUIDQuery(uuid));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void handle_lookupScopedToCurrentStudent() {
        // given
        final Student otherStudent = new Student(new CreateStudentCommand("other"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(otherStudent);
        when(repository.findByUuidAndStudent(any(), any())).thenReturn(Optional.empty());

        // when
        sut.handle(new CourseEnrollmentByUUIDQuery(enrollment.getUuid()));

        // then
        verify(repository).findByUuidAndStudent(enrollment.getUuid(), otherStudent);
    }
}
