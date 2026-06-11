package com.educational.platform.course.enrollments.register;

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

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link RegisterStudentToCourseCommandHandler} propagates
 * {@link ConstraintViolationException} from the factory without publishing an event.
 */
@ExtendWith(MockitoExtension.class)
public class RegisterStudentToCourseCommandHandlerConstraintViolationTest {

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
    void handle_factoryThrowsConstraintViolation_propagatesExceptionAndNoEventPublished() {
        // given
        final UUID courseId = UUID.randomUUID();
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(courseEnrollmentFactory.createFrom(command)).thenThrow(new ConstraintViolationException(Set.of()));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(eventPublisher);
    }
}
