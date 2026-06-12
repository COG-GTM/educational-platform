package com.educational.platform.course.enrollments.register;

import com.educational.platform.course.enrollments.*;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStudentToCourseCommandHandlerSuccessTest {

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private CourseEnrollmentFactory courseEnrollmentFactory;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RegisterStudentToCourseCommandHandler sut;

    @Test
    void handle_validCommand_eventContainsStudentReference() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);

        final Student student = new Student(new CreateStudentCommand("alice"));
        ReflectionTestUtils.setField(student, "username", "alice");
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(enrollment.getUuid());
        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> eventCaptor =
                ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().courseId()).isEqualTo(courseId);
    }

    @Test
    void handle_validCommand_enrollmentSavedToRepository() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(5, 10);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);

        final Student student = new Student(new CreateStudentCommand("bob"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        verify(courseEnrollmentRepository).save(enrollment);
    }

    @Test
    void handle_twoEnrollments_differentUuidsReturned() {
        // given
        final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final RegisterStudentToCourseCommand command1 = new RegisterStudentToCourseCommand(courseId1);
        final RegisterStudentToCourseCommand command2 = new RegisterStudentToCourseCommand(courseId2);

        final CourseEnrollment enrollment1 = new CourseEnrollment(1, 2);
        final CourseEnrollment enrollment2 = new CourseEnrollment(3, 4);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });
        when(courseEnrollmentFactory.createFrom(command1)).thenReturn(enrollment1);
        when(courseEnrollmentFactory.createFrom(command2)).thenReturn(enrollment2);

        final Student student = new Student(new CreateStudentCommand("charlie"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID uuid1 = sut.handle(command1);
        final UUID uuid2 = sut.handle(command2);

        // then
        assertThat(uuid1).isNotEqualTo(uuid2);
    }
}
