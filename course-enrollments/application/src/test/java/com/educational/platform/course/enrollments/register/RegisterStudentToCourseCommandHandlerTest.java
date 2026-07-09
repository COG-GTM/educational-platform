package com.educational.platform.course.enrollments.register;

import com.educational.platform.common.outbox.IntegrationEventOutbox;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.enrollments.student.Student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStudentToCourseCommandHandlerTest {

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private CourseEnrollmentRepository repository;

    @Mock
    private CourseEnrollmentFactory factory;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    @Mock
    private IntegrationEventOutbox integrationEventOutbox;

    @Mock
    private Student student;

    private RegisterStudentToCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new RegisterStudentToCourseCommandHandler(new TransactionTemplate(transactionManager), repository, factory, currentUserAsStudent, integrationEventOutbox);
    }

    @Test
    void handle_validCommand_enrollmentSavedAndEventStoredInOutbox() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final CourseEnrollment enrollment = new CourseEnrollment(1, 1);
        when(factory.createFrom(command)).thenReturn(enrollment);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);
        when(student.toReference()).thenReturn("student");

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(enrollment.getUuid());
        verify(repository).save(enrollment);

        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> eventArgument = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(integrationEventOutbox).publish(eventArgument.capture());
        assertThat(eventArgument.getValue())
                .isEqualTo(new StudentEnrolledToCourseIntegrationEvent(courseId, "student"));
    }
}
