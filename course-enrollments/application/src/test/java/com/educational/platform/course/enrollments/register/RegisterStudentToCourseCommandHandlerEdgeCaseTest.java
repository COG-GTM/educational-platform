package com.educational.platform.course.enrollments.register;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.CourseEnrollmentFactory;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStudentToCourseCommandHandlerEdgeCaseTest {

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
    void handle_factoryThrowsRelatedResourceNotResolved_exceptionPropagated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });
        when(courseEnrollmentFactory.createFrom(command))
                .thenThrow(new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + courseId));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(handle);
    }

    @Test
    void handle_factoryThrowsConstraintViolation_exceptionPropagated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });
        when(courseEnrollmentFactory.createFrom(command))
                .thenThrow(new ConstraintViolationException(Set.of()));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }
}
