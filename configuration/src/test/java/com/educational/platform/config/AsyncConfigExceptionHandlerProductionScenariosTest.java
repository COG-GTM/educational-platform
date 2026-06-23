package com.educational.platform.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests that the {@link AsyncConfig}'s uncaught exception handler gracefully handles
 * the production exception types that may escape the retry+recover chain.
 * <p>
 * In normal operation, retryable exceptions are caught by {@code @Recover}. However,
 * non-retryable exceptions (NPE, IllegalStateException, etc.) propagate through the
 * async proxy and reach the uncaught exception handler. These tests verify the handler
 * does not re-throw or crash, which would break the async infrastructure.
 * <p>
 * Scenario: Handler throws NPE → not retryable → not recovered → propagates to async
 * exception handler → must be logged without crashing the executor thread.
 */
class AsyncConfigExceptionHandlerProductionScenariosTest {

    private AsyncUncaughtExceptionHandler handler;
    private Method sampleMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        handler = new AsyncConfig().getAsyncUncaughtExceptionHandler();
        sampleMethod = getClass().getDeclaredMethod("setUp");
    }

    @Test
    void handlesNullPointerException_fromHandlerWithNullEvent() {
        // NPE when handler receives null event (event.getClass() throws)
        NullPointerException npe = new NullPointerException("Cannot invoke method on null reference");
        assertThatCode(() -> handler.handleUncaughtException(npe, sampleMethod, (Object) null))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesIllegalStateException_fromBusinessLogicFailure() {
        // Business logic exceptions that bypass retry (not in retryFor list)
        IllegalStateException ise = new IllegalStateException("Course is not in PENDING state");
        assertThatCode(() -> handler.handleUncaughtException(ise, sampleMethod, "event-payload"))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesDataAccessException_thatSomehowEscapedRecovery() {
        // Edge case: DataAccessException escapes if @Recover itself throws
        DataAccessResourceFailureException dae = new DataAccessResourceFailureException("DB pool exhausted");
        assertThatCode(() -> handler.handleUncaughtException(dae, sampleMethod, "event-123"))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesOptimisticLockException_thatEscapedRecovery() {
        // OOLFE that escapes retry+recover chain
        ObjectOptimisticLockingFailureException oolfe =
                new ObjectOptimisticLockingFailureException("Version mismatch", new RuntimeException("cause"));
        assertThatCode(() -> handler.handleUncaughtException(oolfe, sampleMethod, "event-456"))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesRuntimeExceptionFromRecoverMethod() {
        // When recover() itself fails (e.g., repository.save() throws), the exception
        // propagates through the async proxy to this handler
        RuntimeException recoverFailure = new RuntimeException("Failed to persist dead-letter record",
                new DataAccessResourceFailureException("Connection refused"));
        assertThatCode(() -> handler.handleUncaughtException(recoverFailure, sampleMethod, "event-789"))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesExceptionWithVeryLongMessage() {
        // Exception messages from SQL errors can be extremely long
        String longMessage = "ORA-00001: unique constraint violated. SQL: INSERT INTO failed_integration_events "
                + "(event_class_name, event_payload, exception_message, ...) VALUES ('" + "x".repeat(2000) + "', ...)";
        RuntimeException longMsgException = new RuntimeException(longMessage);
        assertThatCode(() -> handler.handleUncaughtException(longMsgException, sampleMethod, "event"))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesExceptionWithMultipleParams() {
        // Handlers receive a single event parameter, but verify multi-param safety
        RuntimeException exception = new RuntimeException("multi-param scenario");
        assertThatCode(() -> handler.handleUncaughtException(exception, sampleMethod,
                "param1", "param2", "param3", null))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesStackOverflowError_propagatedThroughAsyncProxy() {
        // Errors bypass the handler's catch(Exception) block and reach async handler
        StackOverflowError soe = new StackOverflowError("deep recursion in domain logic");
        assertThatCode(() -> handler.handleUncaughtException(soe, sampleMethod, "event"))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesOutOfMemoryError_gracefully() {
        // OOM might occur during event processing; handler must not worsen the situation
        OutOfMemoryError oom = new OutOfMemoryError("Java heap space");
        assertThatCode(() -> handler.handleUncaughtException(oom, sampleMethod, "event"))
                .doesNotThrowAnyException();
    }
}
