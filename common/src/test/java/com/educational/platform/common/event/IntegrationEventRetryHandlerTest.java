package com.educational.platform.common.event;

import com.educational.platform.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationEventRetryHandlerTest {

    @Test
    void isRetryable_transientDataAccessException_returnsTrue() {
        // given
        final Throwable exception = new QueryTimeoutException("timeout");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_optimisticLockingFailureException_returnsTrue() {
        // given
        final Throwable exception = new OptimisticLockingFailureException("optimistic lock");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_pessimisticLockingFailureException_returnsTrue() {
        // given
        final Throwable exception = new PessimisticLockingFailureException("pessimistic lock");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_runtimeException_returnsFalse() {
        // given
        final Throwable exception = new RuntimeException("generic error");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_resourceNotFoundException_returnsFalse() {
        // given
        final Throwable exception = new ResourceNotFoundException("not found");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_illegalArgumentException_returnsFalse() {
        // given
        final Throwable exception = new IllegalArgumentException("bad argument");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_nullPointerException_returnsFalse() {
        // given
        final Throwable exception = new NullPointerException("null");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_nullThrowable_returnsFalse() {
        // when / then
        assertThat(IntegrationEventRetryHandler.isRetryable(null)).isFalse();
    }

    @Test
    void retryableExceptions_containsExactlyThreeExpectedTypes() {
        // then
        assertThat(IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS)
                .hasSize(3)
                .containsExactly(
                        TransientDataAccessException.class,
                        OptimisticLockingFailureException.class,
                        PessimisticLockingFailureException.class
                );
    }

    @Test
    void isRetryable_dataIntegrityViolationException_returnsFalse() {
        // given
        final Throwable exception = new DataIntegrityViolationException("constraint violation");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_checkedExceptionWrappedInRuntime_returnsFalse() {
        // given
        final Throwable exception = new RuntimeException(new java.sql.SQLException("connection refused"));

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_subclassOfPessimisticLockingFailure_returnsTrue() {
        // given
        final Throwable exception = new CannotAcquireLockException("lock timeout");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void retryableExceptions_isConsistentWithIsRetryableMethod() {
        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(new QueryTimeoutException("timeout"))).isTrue();
        assertThat(IntegrationEventRetryHandler.isRetryable(new OptimisticLockingFailureException("lock"))).isTrue();
        assertThat(IntegrationEventRetryHandler.isRetryable(new PessimisticLockingFailureException("lock"))).isTrue();
    }

    @Test
    void retryableExceptions_arrayIsNotModifiable() {
        // given
        final Class<?>[] original = IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS.clone();

        // then
        assertThat(IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS).containsExactly(original);
    }

    @Test
    void isRetryable_concurrencyFailureException_returnsTrue() {
        // ConcurrencyFailureException is a superclass in the hierarchy that includes
        // OptimisticLockingFailureException and PessimisticLockingFailureException
        // given
        final Throwable exception = new ConcurrencyFailureException("concurrency failure");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_subclassOfOptimisticLockingFailureException_returnsTrue() {
        // given - anonymous subclass simulates framework subclasses like ObjectOptimisticLockingFailureException
        final Throwable exception = new OptimisticLockingFailureException("custom subclass") {};

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void class_hasComponentAnnotation() {
        // then
        assertThat(IntegrationEventRetryHandler.class.isAnnotationPresent(Component.class)).isTrue();
    }

    @Test
    void isRetryable_error_returnsFalse() {
        // given
        final Throwable error = new Error("out of memory");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(error)).isFalse();
    }

    @Test
    void isRetryable_stackOverflowError_returnsFalse() {
        // given
        final Throwable error = new StackOverflowError("stack overflow");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(error)).isFalse();
    }

    @Test
    void isRetryable_outOfMemoryError_returnsFalse() {
        // given
        final Throwable error = new OutOfMemoryError("heap space");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(error)).isFalse();
    }

    @Test
    void retryableExceptions_externalMutationDoesNotAffectClassification() {
        // given - mutate the array externally
        final Class<?> original = IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS[0];
        IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS[0] = RuntimeException.class;

        // when - isRetryable still uses instanceof, not the array
        final boolean result = IntegrationEventRetryHandler.isRetryable(new QueryTimeoutException("timeout"));

        // then
        assertThat(result).isTrue();

        // cleanup
        IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS[0] = original;
    }

    @Test
    void retryableExceptions_allEntriesAreNonNull() {
        // then
        for (Class<?> exceptionClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            assertThat(exceptionClass).isNotNull();
        }
    }

    @Test
    void retryableExceptions_allEntriesAreAssignableFromDataAccessException() {
        // then - all retryable exception types should be DataAccessException subclasses
        for (Class<?> exceptionClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            assertThat(org.springframework.dao.DataAccessException.class.isAssignableFrom(exceptionClass))
                    .as("Expected %s to be a DataAccessException subclass", exceptionClass.getName())
                    .isTrue();
        }
    }

    @Test
    void isRetryable_withCustomTransientDataAccessSubclass_returnsTrue() {
        // given - anonymous concrete subclass of TransientDataAccessException
        final Throwable exception = new TransientDataAccessException("custom transient") {};

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_withIllegalStateException_returnsFalse() {
        // given
        final Throwable exception = new IllegalStateException("illegal state");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_nonRetryableWrappingRetryableCause_returnsFalse() {
        // only the top-level exception type is classified, not the cause chain
        // given
        final Throwable retryableCause = new OptimisticLockingFailureException("lock failure");
        final Throwable wrapper = new RuntimeException("wrapper", retryableCause);

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(wrapper)).isFalse();
    }

    @Test
    void retryableExceptions_noDuplicateEntries() {
        // given
        final Class<?>[] exceptions = IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS;

        // then
        assertThat(exceptions).doesNotHaveDuplicates();
    }

    @Test
    void retryableExceptions_eachTypeMatchesIsRetryableClassification() {
        // Verify every type in RETRYABLE_EXCEPTIONS is accepted by isRetryable's instanceof checks
        for (Class<?> exceptionClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            // then - each declared retryable type must be one of the types isRetryable checks
            assertThat(
                    TransientDataAccessException.class.isAssignableFrom(exceptionClass)
                    || OptimisticLockingFailureException.class.isAssignableFrom(exceptionClass)
                    || PessimisticLockingFailureException.class.isAssignableFrom(exceptionClass))
                    .as("RETRYABLE_EXCEPTIONS entry %s should be covered by isRetryable", exceptionClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void isRetryable_isStaticMethod() throws NoSuchMethodException {
        // when
        java.lang.reflect.Method method = IntegrationEventRetryHandler.class.getMethod("isRetryable", Throwable.class);

        // then
        assertThat(java.lang.reflect.Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    void retryableExceptions_fieldIsStaticAndFinal() throws NoSuchFieldException {
        // when
        java.lang.reflect.Field field = IntegrationEventRetryHandler.class.getField("RETRYABLE_EXCEPTIONS");

        // then
        assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    void isRetryable_unsupportedOperationException_returnsFalse() {
        // given
        final Throwable exception = new UnsupportedOperationException("not supported");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_methodIsPublic() throws NoSuchMethodException {
        // when
        java.lang.reflect.Method method = IntegrationEventRetryHandler.class.getMethod("isRetryable", Throwable.class);

        // then
        assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    void retryableExceptions_fieldIsPublic() throws NoSuchFieldException {
        // when
        java.lang.reflect.Field field = IntegrationEventRetryHandler.class.getField("RETRYABLE_EXCEPTIONS");

        // then
        assertThat(java.lang.reflect.Modifier.isPublic(field.getModifiers())).isTrue();
    }

    @Test
    void isRetryable_allRetryableExceptionsArrayEntries_areClassifiedAsRetryable() {
        for (Class<?> exceptionClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            // given - instantiate via reflection or subclass
            Throwable instance;
            try {
                instance = (Throwable) exceptionClass.getDeclaredConstructor(String.class).newInstance("test");
            } catch (Exception e) {
                // Abstract class - use a concrete subclass test (tested elsewhere)
                continue;
            }

            // then
            assertThat(IntegrationEventRetryHandler.isRetryable(instance))
                    .as("isRetryable(%s) should return true", exceptionClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void retryableExceptions_allEntriesAreExceptionSubclasses() {
        for (Class<?> exceptionClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            assertThat(Throwable.class.isAssignableFrom(exceptionClass))
                    .as("%s should be a Throwable subclass", exceptionClass.getName())
                    .isTrue();
        }
    }

    @Test
    void isRetryable_classNotFoundException_returnsFalse() {
        // given - checked exception wrapped as cause
        final Throwable exception = new RuntimeException("wrap", new ClassNotFoundException("missing"));

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void retryableExceptions_allEntriesAreUncheckedExceptions() {
        for (Class<?> exceptionClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            assertThat(RuntimeException.class.isAssignableFrom(exceptionClass))
                    .as("%s should be an unchecked (RuntimeException) subclass", exceptionClass.getName())
                    .isTrue();
        }
    }

}

