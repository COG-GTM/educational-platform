package com.educational.platform.course.enrollments.register;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
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
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        sut = new RegisterStudentToCourseCommandHandler(transactionTemplate, courseEnrollmentRepository, courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_validCommand_enrollmentSavedAndEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);
        when(currentUserAsStudent.userAsStudent()).thenReturn(new Student(new CreateStudentCommand("username")));

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(enrollment.getUuid());
        verify(courseEnrollmentRepository).save(enrollment);

        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> eventArgument = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        final StudentEnrolledToCourseIntegrationEvent event = eventArgument.getValue();
        assertThat(event)
                .hasFieldOrPropertyWithValue("courseId", courseId)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handle_validCommand_eventPublishedBeforeTransactionCommit() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(1, 2);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);
        when(currentUserAsStudent.userAsStudent()).thenReturn(new Student(new CreateStudentCommand("username")));

        // when
        sut.handle(command);

        // then
        final InOrder inOrder = inOrder(eventPublisher, transactionManager);
        inOrder.verify(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
        inOrder.verify(transactionManager).commit(any());
    }

}
