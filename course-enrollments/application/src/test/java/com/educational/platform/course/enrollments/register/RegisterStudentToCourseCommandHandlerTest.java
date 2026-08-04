package com.educational.platform.course.enrollments.register;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStudentToCourseCommandHandlerTest {

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

    @Mock
    private CourseEnrollment courseEnrollment;

    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private RegisterStudentToCourseCommandHandler sut;

    @Test
    void handle_validCommand_studentEnrolledToCourseIntegrationEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID enrollmentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final Student student = new Student(new CreateStudentCommand("username"));

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            final TransactionCallback<CourseEnrollment> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(courseEnrollment);
        when(courseEnrollment.getUuid()).thenReturn(enrollmentUuid);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(enrollmentUuid);
        verify(courseEnrollmentRepository).save(courseEnrollment);
        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> argument = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(argument.capture());
        final StudentEnrolledToCourseIntegrationEvent event = argument.getValue();
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.username()).isEqualTo("username");
    }

}
