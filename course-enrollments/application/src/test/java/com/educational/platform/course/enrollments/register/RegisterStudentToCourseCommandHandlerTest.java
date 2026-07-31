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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        sut = new RegisterStudentToCourseCommandHandler(new TransactionTemplate(transactionManager), courseEnrollmentRepository, courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_validCommand_enrollmentSavedAndEventPublishedWithStudentUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID studentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CourseEnrollment enrollment = new CourseEnrollment(11, 21);
        when(courseEnrollmentFactory.createFrom(command)).thenReturn(enrollment);

        final Student student = new Student(new CreateStudentCommand(studentUuid, "username"));
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isEqualTo(enrollment.getUuid());
        verify(courseEnrollmentRepository).save(enrollment);

        final ArgumentCaptor<StudentEnrolledToCourseIntegrationEvent> argument = ArgumentCaptor.forClass(StudentEnrolledToCourseIntegrationEvent.class);
        verify(eventPublisher).publishEvent(argument.capture());
        final StudentEnrolledToCourseIntegrationEvent event = argument.getValue();
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.studentId()).isEqualTo(studentUuid);
    }
}
