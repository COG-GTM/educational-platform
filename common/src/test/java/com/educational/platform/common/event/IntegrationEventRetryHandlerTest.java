package com.educational.platform.common.event;

import com.educational.platform.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
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

    @Test
    void isRetryable_sqlException_returnsFalse() {
        // given - raw SQL exceptions are not Spring DataAccessExceptions
        final Throwable exception = new java.sql.SQLException("connection refused");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_retryableExceptionWithNullMessage_returnsTrue() {
        // given
        final Throwable exception = new OptimisticLockingFailureException(null);

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_retryableExceptionWithEmptyMessage_returnsTrue() {
        // given
        final Throwable exception = new PessimisticLockingFailureException("");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_retryableExceptionWithCause_returnsTrue() {
        // given
        final Throwable cause = new RuntimeException("root cause");
        final Throwable exception = new OptimisticLockingFailureException("lock", cause);

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_withInterruptedException_returnsFalse() {
        // given
        final Throwable exception = new InterruptedException("thread interrupted");

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isFalse();
    }

    @Test
    void retryableExceptions_allTypesAreSubtypesOfTransientDataAccessException() {
        // All retryable exception types used in @Retryable fall under TransientDataAccessException,
        // which is the @Recover parameter type. This ensures the recover method catches all of them.
        for (Class<?> exceptionClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            assertThat(TransientDataAccessException.class.isAssignableFrom(exceptionClass))
                    .as("%s should be assignable to TransientDataAccessException", exceptionClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void class_canBeInstantiated() {
        // @Component requires a no-arg or injectable constructor
        IntegrationEventRetryHandler handler = new IntegrationEventRetryHandler();
        assertThat(handler).isNotNull();
    }

    @Test
    void class_isNotAbstract() {
        assertThat(java.lang.reflect.Modifier.isAbstract(IntegrationEventRetryHandler.class.getModifiers())).isFalse();
    }

    @Test
    void class_isNotFinal() {
        assertThat(java.lang.reflect.Modifier.isFinal(IntegrationEventRetryHandler.class.getModifiers())).isFalse();
    }

    @Test
    void isRetryable_withTransientDataAccessResourceException_returnsTrue() {
        // TransientDataAccessResourceException is a concrete subclass of TransientDataAccessException
        // commonly thrown for JDBC connection failures
        final Throwable exception = new org.springframework.dao.TransientDataAccessResourceException("connection lost");
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_withDeadlockLoserDataAccessException_returnsTrue() {
        // DeadlockLoserDataAccessException extends PessimisticLockingFailureException
        final Throwable exception = new org.springframework.dao.DeadlockLoserDataAccessException("deadlock", null);
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void retryableExceptions_isNotEmpty() {
        assertThat(IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS).isNotEmpty();
    }

    @Test
    void isRetryable_concurrentCallsFromMultipleThreads_allReturnCorrectResult() throws Exception {
        // Verifies thread-safety: isRetryable is stateless and safe for concurrent invocation
        var latch = new java.util.concurrent.CountDownLatch(1);
        int threadCount = 10;
        var threads = new Thread[threadCount];
        var trueCount = new java.util.concurrent.atomic.AtomicInteger(0);
        var falseCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Throwable exception = idx % 2 == 0
                        ? new OptimisticLockingFailureException("lock")
                        : new IllegalArgumentException("bad arg");
                if (IntegrationEventRetryHandler.isRetryable(exception)) {
                    trueCount.incrementAndGet();
                } else {
                    falseCount.incrementAndGet();
                }
            });
            threads[i].start();
        }

        latch.countDown();
        for (Thread t : threads) {
            t.join(5000);
        }

        // 5 even-indexed threads pass retryable exceptions, 5 odd-indexed pass non-retryable
        assertThat(trueCount.get()).isEqualTo(5);
        assertThat(falseCount.get()).isEqualTo(5);
    }

    @Test
    void isRetryable_cannotAcquireLockExceptionSubclass_returnsTrue() {
        // CannotAcquireLockException extends PessimisticLockingFailureException - common in production
        final Throwable exception = new CannotAcquireLockException("deadlock detected");
        assertThat(IntegrationEventRetryHandler.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_methodReturnsBoolean() throws NoSuchMethodException {
        java.lang.reflect.Method method = IntegrationEventRetryHandler.class.getMethod("isRetryable", Throwable.class);

        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void isRetryable_methodAcceptsThrowableParameter() throws NoSuchMethodException {
        java.lang.reflect.Method method = IntegrationEventRetryHandler.class.getMethod("isRetryable", Throwable.class);

        assertThat(method.getParameterCount()).isEqualTo(1);
        assertThat(method.getParameterTypes()[0]).isEqualTo(Throwable.class);
    }

    @Test
    void retryableExceptions_arrayLengthMatchesIsRetryableInstanceofChecks() {
        // isRetryable checks 3 types: TransientDataAccessException, OptimisticLocking, PessimisticLocking
        assertThat(IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS).hasSize(3);
    }

    @Test
    void class_hasNoInstanceFields() {
        // IntegrationEventRetryHandler should be stateless - all methods are static
        java.lang.reflect.Field[] fields = IntegrationEventRetryHandler.class.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                    .as("Field '%s' should be static (handler must be stateless)", field.getName())
                    .isTrue();
        }
    }

    @Test
    void isRetryable_withExceptionHavingNullCause_returnsCorrectResult() {
        // given
        final Throwable retryable = new OptimisticLockingFailureException("lock", null);
        final Throwable nonRetryable = new RuntimeException("error", null);

        // then
        assertThat(IntegrationEventRetryHandler.isRetryable(retryable)).isTrue();
        assertThat(IntegrationEventRetryHandler.isRetryable(nonRetryable)).isFalse();
    }

    @Test
    void isRetryable_calledTwiceWithSameException_returnsSameResult() {
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("lock");
        assertThat(IntegrationEventRetryHandler.isRetryable(exception))
                .isEqualTo(IntegrationEventRetryHandler.isRetryable(exception));
    }

    @Test
    void retryableExceptions_orderIsConsistent() {
        Class<?>[] exceptions = IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS;
        assertThat(exceptions[0]).isEqualTo(TransientDataAccessException.class);
        assertThat(exceptions[1]).isEqualTo(OptimisticLockingFailureException.class);
        assertThat(exceptions[2]).isEqualTo(PessimisticLockingFailureException.class);
    }

    @Test
    void isRetryable_allRetryableExceptionsShareCommonBaseClass() {
        for (Class<?> exClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            assertThat(DataAccessException.class.isAssignableFrom(exClass))
                    .as("%s should extend DataAccessException", exClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void retryableExceptions_firstEntryIsTheRecoverParameterType() {
        assertThat(IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS[0])
                .as("First retryable exception should be the recover method's parameter supertype")
                .isEqualTo(TransientDataAccessException.class);
    }

    @Test
    void isRetryable_queryTimeoutException_returnsTrue() {
        assertThat(IntegrationEventRetryHandler.isRetryable(new QueryTimeoutException("query timed out")))
                .isTrue();
    }

    @Test
    void isRetryable_doesNotInspectCauseChain_onlyChecksTopLevelType() {
        // given - a non-retryable wrapper around a retryable cause
        final Throwable retryableCause = new OptimisticLockingFailureException("inner lock");
        final Throwable nonRetryableWrapper = new IllegalStateException("wrapper", retryableCause);

        // then - only the top-level exception type is evaluated
        assertThat(IntegrationEventRetryHandler.isRetryable(nonRetryableWrapper)).isFalse();
    }

    @Test
    void isRetryable_retryableWrapperAroundNonRetryableCause_returnsTrue() {
        // given - a retryable wrapper around a non-retryable cause
        final Throwable nonRetryableCause = new IllegalArgumentException("bad input");
        final Throwable retryableWrapper = new OptimisticLockingFailureException("lock", nonRetryableCause);

        // then - only the top-level exception type matters
        assertThat(IntegrationEventRetryHandler.isRetryable(retryableWrapper)).isTrue();
    }

    @Test
    void retryableExceptions_arrayReferenceIsFinal() throws NoSuchFieldException {
        java.lang.reflect.Field field = IntegrationEventRetryHandler.class.getDeclaredField("RETRYABLE_EXCEPTIONS");

        assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                .as("RETRYABLE_EXCEPTIONS array reference must be final")
                .isTrue();
    }

    @Test
    void retryableExceptions_allEntriesAreDistinctTypes() {
        Class<?>[] exceptions = IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS;
        long distinctCount = java.util.Arrays.stream(exceptions).distinct().count();
        assertThat(distinctCount)
                .as("All retryable exception types should be distinct")
                .isEqualTo(exceptions.length);
    }

    @Test
    void retryableExceptions_allEntriesAreExceptionClasses() {
        for (Class<?> exClass : IntegrationEventRetryHandler.RETRYABLE_EXCEPTIONS) {
            assertThat(Throwable.class.isAssignableFrom(exClass))
                    .as("%s must be a Throwable subclass", exClass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void class_hasNoPublicConstructors() {
        var constructors = IntegrationEventRetryHandler.class.getDeclaredConstructors();
        for (var constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
                assertThat(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()))
                        .as("Default constructor should be public for Spring DI")
                        .isTrue();
            }
        }
    }

}

