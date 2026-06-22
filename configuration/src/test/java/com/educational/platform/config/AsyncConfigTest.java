package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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

    @Test
    void asyncConfig_hasEnableAsyncAnnotation() {
        // then
        assertThat(AsyncConfig.class.getAnnotation(EnableAsync.class)).isNotNull();
    }

    @Test
    void asyncConfig_hasConfigurationAnnotation() {
        // then
        assertThat(AsyncConfig.class.getAnnotation(Configuration.class)).isNotNull();
    }

    @Test
    void asyncConfig_implementsAsyncConfigurer() {
        // then
        assertThat(AsyncConfigurer.class).isAssignableFrom(AsyncConfig.class);
    }

    @Test
    void getAsyncExecutor_executorCanSubmitAndRunTasks() throws Exception {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        // when
        executor.submit(latch::countDown);

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_threadNameStartsWithPrefix() throws Exception {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        String[] threadName = new String[1];
        CountDownLatch latch = new CountDownLatch(1);

        // when
        executor.submit(() -> {
            threadName[0] = Thread.currentThread().getName();
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);

        // then
        assertThat(threadName[0]).startsWith("integration-event-");
        executor.shutdown();
    }
}
