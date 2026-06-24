package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void getAsyncExecutor_returnsConfiguredExecutor() {
        // when
        Executor executor = asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    void getAsyncExecutor_hasCorrectThreadNamePrefix() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getThreadNamePrefix()).isEqualTo("integration-event-");
    }

    @Test
    void getAsyncExecutor_hasCorrectPoolSizes() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaxPoolSize()).isEqualTo(8);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_returnsHandler() {
        // when
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();

        // then
        assertThat(handler).isNotNull();
        assertThat(handler).isInstanceOf(AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler.class);
    }

    @Test
    void asyncUncaughtExceptionHandler_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException("test error");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "param1", "param2"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_withNoParams_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException("test error");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_withNullMessage_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException((String) null);

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "param1"))
                .doesNotThrowAnyException();
    }

}
