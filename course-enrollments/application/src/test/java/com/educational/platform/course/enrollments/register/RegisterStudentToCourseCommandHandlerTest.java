package com.educational.platform.course.enrollments.register;

import com.educational.platform.contracts.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RegisterStudentToCourseCommandHandlerTest {

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private CourseEnrollmentFactory courseEnrollmentFactory;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegisterStudentToCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new RegisterStudentToCourseCommandHandler(new TransactionTemplate(transactionManager),
                courseEnrollmentRepository, courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_validCommand_enrollmentSavedAndStudentEnrolledEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);
        when(currentUserAsStudent.userAsStudent()).thenReturn(new Student(new CreateStudentCommand("student-username")));

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(enrollment.getUuid());
        verify(courseEnrollmentRepository).save(enrollment);
        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> argument =
                ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(argument.capture());
        final StudentEnrolledToCourseIntegrationEvent event = argument.getValue();
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.username()).isEqualTo("student-username");
    }

    @Test
    void handle_validCommand_eventPublishedAfterTransactionCommit() {
        // given
        final RegisterStudentToCourseCommand command =
                new RegisterStudentToCourseCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(new CourseEnrollment(1, 2));
        when(currentUserAsStudent.userAsStudent()).thenReturn(new Student(new CreateStudentCommand("student-username")));

        // when
        sut.handle(command);

        // then
        final InOrder inOrder = inOrder(transactionManager, eventPublisher);
        inOrder.verify(transactionManager).commit(any());
        inOrder.verify(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

}
