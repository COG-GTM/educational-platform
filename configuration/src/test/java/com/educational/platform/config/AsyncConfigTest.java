package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void getAsyncExecutor_returnsThreadPoolWithCorrectPrefix() {
        // when
        Executor executor = asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("integration-event-");
    }

    @Test
    void getAsyncExecutor_returnsThreadPoolWithCorrectPoolSizes() {
        // when
        Executor executor = asyncConfig.getAsyncExecutor();

        // then
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(4);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(8);
    }

    @Test
    void getAsyncExecutor_returnsThreadPoolWithCorrectQueueCapacity() {
        // when
        Executor executor = asyncConfig.getAsyncExecutor();

        // then
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(100);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_returnsNonNullHandler() {
        // when
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();

        // then
        assertThat(handler).isNotNull();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesExceptionWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException("test exception");
        var method = AsyncConfigTest.class.getDeclaredMethod("asyncUncaughtExceptionHandler_handlesExceptionWithoutThrowing");

        // when/then - should not throw
        handler.handleUncaughtException(exception, method, "param1", "param2");
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesNullException() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        var method = AsyncConfigTest.class.getDeclaredMethod("asyncUncaughtExceptionHandler_handlesNullException");

        // when/then - should not throw even with null throwable
        handler.handleUncaughtException(new NullPointerException(), method);
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesEmptyParams() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException("test");
        var method = AsyncConfigTest.class.getDeclaredMethod("asyncUncaughtExceptionHandler_handlesEmptyParams");

        // when/then - should not throw with no params
        handler.handleUncaughtException(exception, method);
    }
}
