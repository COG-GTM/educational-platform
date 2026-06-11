package com.educational.platform.course.enrollments.register;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RegisterStudentToCourseCommandHandlerEdgeCasesTest {

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
        sut = new RegisterStudentToCourseCommandHandler(transactionTemplate, courseEnrollmentRepository,
                courseEnrollmentFactory, currentUserAsStudent, eventPublisher);
    }

    @Test
    void handle_factoryThrowsRelatedResourceNotResolved_propagatesException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(courseEnrollmentFactory.createFrom(command)).thenThrow(
                new RelatedResourceIsNotResolvedException("Course not found"));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class)
                .isThrownBy(handle)
                .withMessageContaining("Course not found");
        verifyNoInteractions(eventPublisher);
    }
}
