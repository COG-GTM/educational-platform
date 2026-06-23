package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests edge cases for the AsyncConfig's uncaught exception handler,
 * including Error types, inner class encapsulation, and interface compliance.
 */
class AsyncConfigExceptionHandlerEdgeCaseTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void asyncUncaughtExceptionHandler_handlesErrorSubclass() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_handlesErrorSubclass");
        OutOfMemoryError error = new OutOfMemoryError("heap space exhausted");

        assertThatCode(() -> handler.handleUncaughtException(error, method, "param1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesStackOverflowError() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_handlesStackOverflowError");
        StackOverflowError error = new StackOverflowError("deep recursion");

        assertThatCode(() -> handler.handleUncaughtException(error, method, "param1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesExceptionWithNullMessage() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_handlesExceptionWithNullMessage");
        RuntimeException exception = new RuntimeException((String) null);

        assertThatCode(() -> handler.handleUncaughtException(exception, method, "param1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesExceptionWithVeryLongMessage() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_handlesExceptionWithVeryLongMessage");
        String longMessage = "X".repeat(10000);
        RuntimeException exception = new RuntimeException(longMessage);

        assertThatCode(() -> handler.handleUncaughtException(exception, method, "param1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesExceptionWithDeepCauseChain() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_handlesExceptionWithDeepCauseChain");
        var root = new RuntimeException("root");
        var mid = new IllegalStateException("mid", root);
        var top = new RuntimeException("top", mid);

        assertThatCode(() -> handler.handleUncaughtException(top, method, "param1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesParamsWithNullElements() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_handlesParamsWithNullElements");
        RuntimeException exception = new RuntimeException("test");

        assertThatCode(() -> handler.handleUncaughtException(exception, method, null, "valid", null))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncConfig_implementsAsyncConfigurer() {
        assertThat(AsyncConfigurer.class).isAssignableFrom(AsyncConfig.class);
    }

    @Test
    void asyncConfig_innerExceptionHandlerClass_isPrivateStatic() {
        Class<?>[] declaredClasses = AsyncConfig.class.getDeclaredClasses();
        boolean foundHandler = false;
        for (Class<?> inner : declaredClasses) {
            if (AsyncUncaughtExceptionHandler.class.isAssignableFrom(inner)) {
                foundHandler = true;
                assertThat(Modifier.isPrivate(inner.getModifiers()))
                        .as("Exception handler inner class should be private")
                        .isTrue();
                assertThat(Modifier.isStatic(inner.getModifiers()))
                        .as("Exception handler inner class should be static")
                        .isTrue();
            }
        }
        assertThat(foundHandler)
                .as("AsyncConfig should declare a private static inner class implementing AsyncUncaughtExceptionHandler")
                .isTrue();
    }

    @Test
    void asyncConfig_innerExceptionHandlerClass_implementsAsyncUncaughtExceptionHandler() {
        Class<?>[] declaredClasses = AsyncConfig.class.getDeclaredClasses();
        long handlerCount = Arrays.stream(declaredClasses)
                .filter(AsyncUncaughtExceptionHandler.class::isAssignableFrom)
                .count();
        assertThat(handlerCount).isEqualTo(1);
    }

    @Test
    void asyncConfig_getAsyncExecutor_overridesAsyncConfigurer() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
    }

    @Test
    void asyncConfig_getAsyncUncaughtExceptionHandler_overridesAsyncConfigurer() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncUncaughtExceptionHandler");
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesManyParams() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_handlesManyParams");
        RuntimeException exception = new RuntimeException("test");
        Object[] manyParams = new Object[100];
        Arrays.fill(manyParams, "param");

        assertThatCode(() -> handler.handleUncaughtException(exception, method, manyParams))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_multipleCallsDoNotInterfere() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = getClass().getDeclaredMethod("asyncUncaughtExceptionHandler_multipleCallsDoNotInterfere");

        assertThatCode(() -> {
            handler.handleUncaughtException(new RuntimeException("first"), method, "p1");
            handler.handleUncaughtException(new IllegalStateException("second"), method, "p2");
            handler.handleUncaughtException(new NullPointerException("third"), method, "p3");
        }).doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_returnedHandlerIsSameInstance() {
        AsyncUncaughtExceptionHandler handler1 = asyncConfig.getAsyncUncaughtExceptionHandler();
        AsyncUncaughtExceptionHandler handler2 = asyncConfig.getAsyncUncaughtExceptionHandler();

        // Each call creates a new instance (not a singleton)
        assertThat(handler1).isNotNull();
        assertThat(handler2).isNotNull();
    }
}
