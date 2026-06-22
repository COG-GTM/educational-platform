package com.educational.platform.config.handler;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the exception type hierarchy assumptions that the @Recover pattern relies on.
 * <p>
 * All integration event handlers declare:
 * <pre>
 * {@literal @}Retryable(retryFor = {DataAccessException.class, ObjectOptimisticLockingFailureException.class}, ...)
 * {@literal @}Recover void recover(DataAccessException e, XxxEvent event) { ... }
 * </pre>
 * The recover method only accepts {@link DataAccessException}. If
 * {@link ObjectOptimisticLockingFailureException} were NOT a subclass of
 * {@link DataAccessException}, the recover method would never be invoked for
 * optimistic-lock failures after retries are exhausted — silently breaking the
 * dead-letter persistence contract.
 * <p>
 * These tests guard against a Spring Framework version upgrade changing the
 * exception hierarchy.
 */
class ExceptionHierarchyVerificationTest {

    @Test
    void objectOptimisticLockingFailureException_isSubclassOfDataAccessException() {
        assertThat(DataAccessException.class)
                .as("ObjectOptimisticLockingFailureException must be assignable to DataAccessException "
                        + "for @Recover(DataAccessException) to handle it")
                .isAssignableFrom(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void dataAccessResourceFailureException_isSubclassOfDataAccessException() {
        assertThat(DataAccessException.class)
                .isAssignableFrom(DataAccessResourceFailureException.class);
    }

    @Test
    void dataIntegrityViolationException_isSubclassOfDataAccessException() {
        assertThat(DataAccessException.class)
                .isAssignableFrom(DataIntegrityViolationException.class);
    }

    @Test
    void objectOptimisticLockingFailureException_instanceOfDataAccessException() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("test", new RuntimeException());
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void recoverMethodParameter_catchesAllRetryForExceptionTypes() {
        DataAccessResourceFailureException transientEx = new DataAccessResourceFailureException("DB down");
        DataIntegrityViolationException integrityEx = new DataIntegrityViolationException("constraint");
        ObjectOptimisticLockingFailureException optimisticEx =
                new ObjectOptimisticLockingFailureException("lock", new RuntimeException());

        assertThat(transientEx).isInstanceOf(DataAccessException.class);
        assertThat(integrityEx).isInstanceOf(DataAccessException.class);
        assertThat(optimisticEx).isInstanceOf(DataAccessException.class);
    }

    @Test
    void objectOptimisticLockingFailureException_getMessage_preservesMessage() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("specific lock message", new RuntimeException("cause"));
        assertThat(ex.getMessage()).isEqualTo("specific lock message");
    }

    @Test
    void dataAccessResourceFailureException_getMessage_preservesNullMessage() {
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException((String) null);
        assertThat(ex.getMessage()).isNull();
    }
}
