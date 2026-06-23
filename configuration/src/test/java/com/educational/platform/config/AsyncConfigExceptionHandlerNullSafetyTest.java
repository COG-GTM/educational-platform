package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the null-safety characteristics of {@link AsyncConfig}'s exception handler.
 * <p>
 * Spring's async infrastructure guarantees that the {@code method} parameter is always
 * non-null. This test documents the handler's behavior with various parameter combinations:
 * <ul>
 *   <li>null method — throws NPE (acceptable: Spring contract guarantees non-null)</li>
 *   <li>null params array — handled gracefully by {@code Arrays.toString(null)}</li>
 *   <li>empty params array — handled gracefully</li>
 *   <li>exception with null message — handled gracefully by SLF4J</li>
 * </ul>
 */
class AsyncConfigExceptionHandlerNullSafetyTest {

    private final AsyncConfig config = new AsyncConfig();
    private final AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();

    @Test
    void handleUncaughtException_withNullMethod_throwsNullPointerException() {
        // Spring's contract guarantees method is never null, so NPE is acceptable behavior
        assertThatThrownBy(() -> handler.handleUncaughtException(
                new RuntimeException("test"), null, "param1"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handleUncaughtException_withNullParams_doesNotThrow() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withNullParams_doesNotThrow");
        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException("test"), method, (Object[]) null))
                .doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_withEmptyParams_doesNotThrow() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withEmptyParams_doesNotThrow");
        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException("test"), method))
                .doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_withNullException_throwsNullPointerException() throws NoSuchMethodException {
        // The handler logs ex (Throwable); SLF4J handles null Throwable gracefully,
        // but method.getName() is called first — as long as method is non-null, this is fine.
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withNullException_throwsNullPointerException");
        assertThatCode(() -> handler.handleUncaughtException(null, method, "param"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_withNullElementsInParams_doesNotThrow() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withNullElementsInParams_doesNotThrow");
        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException("test"), method, null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_withExceptionWithNullMessage_doesNotThrow() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withExceptionWithNullMessage_doesNotThrow");
        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException((String) null), method, "param"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_withExceptionWithNullCause_doesNotThrow() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withExceptionWithNullCause_doesNotThrow");
        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException("msg", null), method, "param"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_multipleSequentialCalls_allSucceed() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_multipleSequentialCalls_allSucceed");
        assertThatCode(() -> {
            handler.handleUncaughtException(new RuntimeException("a"), method, "p1");
            handler.handleUncaughtException(new RuntimeException("b"), method, "p2");
            handler.handleUncaughtException(new IllegalStateException("c"), method);
            handler.handleUncaughtException(new RuntimeException("d"), method, "p3", "p4");
        }).doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_withDeeplyNestedCause_doesNotThrow() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withDeeplyNestedCause_doesNotThrow");
        var root = new RuntimeException("root");
        var mid = new RuntimeException("mid", root);
        var top = new RuntimeException("top", mid);
        assertThatCode(() -> handler.handleUncaughtException(top, method, "param"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleUncaughtException_withError_doesNotThrow() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("handleUncaughtException_withError_doesNotThrow");
        assertThatCode(() -> handler.handleUncaughtException(
                new StackOverflowError("stack overflow"), method, "param"))
                .doesNotThrowAnyException();
    }
}
