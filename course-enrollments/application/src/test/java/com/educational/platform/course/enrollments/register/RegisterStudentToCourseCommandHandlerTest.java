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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegisterStudentToCourseCommandHandlerTest {

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

    private RegisterStudentToCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new RegisterStudentToCourseCommandHandler(transactionTemplate, courseEnrollmentRepository, courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_validCommand_enrollmentCreatedAndEventPublished() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(courseEnrollment.getUuid());
        verify(eventPublisher).publishEvent(any(StudentEnrolledToCourseIntegrationEvent.class));
    }

    @Test
    void handle_validCommand_integrationEventContainsCorrectData() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment courseEnrollment = new CourseEnrollment(1, 1);

        when(transactionTemplate.execute(any())).thenReturn(courseEnrollment);

        final Student student = new Student(new CreateStudentCommand("student"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        sut.handle(command);

        // then
        ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> eventCaptor = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        final StudentEnrolledToCourseIntegrationEvent event = eventCaptor.getValue();
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.username()).isEqualTo("student");
    }

    @Test
    void handle_transactionReturnsNull_throwsNullPointerException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        when(transactionTemplate.execute(any())).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> sut.handle(command))
                .isInstanceOf(NullPointerException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
