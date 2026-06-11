package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentFactoryTest {

    @Mock
    private EnrollCourseRepository courseRepository;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    private CourseEnrollmentFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseEnrollmentFactory(validator, courseRepository, currentUserAsStudent);
    }

    @Test
    void create_validCommand_courseEnrollmentSaved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final CreateStudentCommand createStudentCommand = new CreateStudentCommand("username");
        final Student correspondingStudent = new Student(createStudentCommand);
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
    }

    @Test
    void createFrom_courseIdIsNull_constraintViolationException() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(null);

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(ConstraintViolationException.class, createAction);
    }

    @Test
    void createFrom_invalidCourseId_relatedResourceIsNotResolvedException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.empty());

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(RelatedResourceIsNotResolvedException.class, createAction);
    }

    @Test
    void create_validCommand_enrollmentHasNonNullUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final CreateStudentCommand createStudentCommand = new CreateStudentCommand("username");
        final Student correspondingStudent = new Student(createStudentCommand);
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then
        assertThat(enrollment.getUuid()).isNotNull();
    }

    @Test
    void createFrom_studentNotFound_throwsNullPointerException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));
        when(currentUserAsStudent.userAsStudent()).thenReturn(null);

        // when
        final Executable createAction = () -> sut.createFrom(command);

        // then
        assertThrows(NullPointerException.class, createAction);
    }

    @Test
    void createFrom_validCommand_queriesRepositoryWithCorrectUuid() {
        // given
        final UUID courseId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final CreateStudentCommand createStudentCommand = new CreateStudentCommand("username");
        final Student correspondingStudent = new Student(createStudentCommand);
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        sut.createFrom(command);

        // then
        verify(courseRepository).findByUuid(courseId);
        verify(currentUserAsStudent).userAsStudent();
    }

    @Test
    void createFrom_courseIdIsNull_constraintViolationContainsCourseIdProperty() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(null);

        // when
        final ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class, () -> sut.createFrom(command));

        // then
        final Set<? extends ConstraintViolation<?>> violations = exception.getConstraintViolations();
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("courseId");
    }

    @Test
    void createFrom_validCommand_enrollmentHasInProgressStatus() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final CreateStudentCommand createStudentCommand = new CreateStudentCommand("username");
        final Student correspondingStudent = new Student(createStudentCommand);
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
        assertThat(enrollment.getUuid()).isNotNull();
    }

    @Test
    void createFrom_courseNotFound_exceptionContainsCourseUuid() {
        // given
        final UUID courseId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.empty());

        // when
        final RelatedResourceIsNotResolvedException exception = assertThrows(
                RelatedResourceIsNotResolvedException.class, () -> sut.createFrom(command));

        // then
        assertThat(exception.getMessage()).contains(courseId.toString());
    }

    @Test
    void createFrom_multipleCalls_produceDifferentUuids() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final CreateStudentCommand createStudentCommand = new CreateStudentCommand("username");
        final Student correspondingStudent = new Student(createStudentCommand);
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment1 = sut.createFrom(command);
        final CourseEnrollment enrollment2 = sut.createFrom(command);

        // then
        assertThat(enrollment1.getUuid()).isNotEqualTo(enrollment2.getUuid());
    }

    @Test
    void createFrom_courseIdIsNull_repositoryNeverQueried() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(null);

        // when
        assertThrows(ConstraintViolationException.class, () -> sut.createFrom(command));

        // then
        verifyNoInteractions(courseRepository);
        verifyNoInteractions(currentUserAsStudent);
    }

    @Test
    void createFrom_validCommand_returnedEnrollmentIsNotNull() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final Student correspondingStudent = new Student(new CreateStudentCommand("username"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then
        assertThat(enrollment).isNotNull();
        assertThat(enrollment.getUuid()).isNotNull();
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
    }

    @Test
    void createFrom_validCommand_enrollmentHasNullEntityIdsBeforePersist() {
        // given — unpersisted entities have null IDs
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(new CreateCourseCommand(courseId));
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final Student correspondingStudent = new Student(new CreateStudentCommand("username"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then — course.getId() and student.getId() are null before persist, so enrollment stores null refs
        assertThat(enrollment).hasFieldOrPropertyWithValue("course", null);
        assertThat(enrollment).hasFieldOrPropertyWithValue("student", null);
    }

    @Test
    void createFrom_validCommand_enrollmentIdIsNullBeforePersist() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(new CreateCourseCommand(courseId));
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final Student correspondingStudent = new Student(new CreateStudentCommand("username"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then
        assertThat(enrollment).hasFieldOrPropertyWithValue("id", null);
    }

}
