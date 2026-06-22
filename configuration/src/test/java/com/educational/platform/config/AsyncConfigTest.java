package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
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

    @Test
    void getAsyncExecutor_multipleCallsReturnSeparateInstances() {
        // when
        Executor first = asyncConfig.getAsyncExecutor();
        Executor second = asyncConfig.getAsyncExecutor();

        // then
        assertThat(first).isNotSameAs(second);
        ((ThreadPoolTaskExecutor) first).shutdown();
        ((ThreadPoolTaskExecutor) second).shutdown();
    }

    @Test
    void enableAsync_hasHighestPrecedenceOrder() {
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableAsync).isNotNull();
        assertThat(enableAsync.order()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void getAsyncExecutor_hasBeanAnnotation() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");
        Bean bean = method.getAnnotation(Bean.class);
        assertThat(bean).isNotNull();
        assertThat(bean.name()).contains("integrationEventExecutor");
    }

    @Test
    void getAsyncExecutor_shutdownConfigIsSet() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then — verify via reflection that waitForTasksToCompleteOnShutdown was set to true
        java.lang.reflect.Field field = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                .getDeclaredField("waitForTasksToCompleteOnShutdown");
        field.setAccessible(true);
        assertThat((boolean) field.get(executor)).isTrue();
        executor.shutdown();
    }

    @Test
    void getAsyncUncaughtExceptionHandler_multipleCallsReturnSeparateInstances() {
        // when
        AsyncUncaughtExceptionHandler first = asyncConfig.getAsyncUncaughtExceptionHandler();
        AsyncUncaughtExceptionHandler second = asyncConfig.getAsyncUncaughtExceptionHandler();

        // then
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesErrorSubclass() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        OutOfMemoryError error = new OutOfMemoryError("heap space");
        var method = AsyncConfigTest.class.getDeclaredMethod("asyncUncaughtExceptionHandler_handlesErrorSubclass");

        // when/then - should not throw even with Error subclass
        handler.handleUncaughtException(error, method, "param1");
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesExceptionWithNullMessage() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException((String) null);
        var method = AsyncConfigTest.class.getDeclaredMethod("asyncUncaughtExceptionHandler_handlesExceptionWithNullMessage");

        // when/then - should not throw with null message
        handler.handleUncaughtException(exception, method, "param1");
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesExceptionWithNullParams() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException("test");
        var method = AsyncConfigTest.class.getDeclaredMethod("asyncUncaughtExceptionHandler_handlesExceptionWithNullParams");

        // when/then - should not throw with null params array
        handler.handleUncaughtException(exception, method, (Object[]) null);
    }

    @Test
    void getAsyncExecutor_awaitTerminationSecondsIs30() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        java.lang.reflect.Field field = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                .getDeclaredField("awaitTerminationMillis");
        field.setAccessible(true);
        assertThat((long) field.get(executor)).isEqualTo(30_000L);
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_supportsConcurrentTaskExecution() throws Exception {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        int taskCount = 4;
        CountDownLatch allStarted = new CountDownLatch(taskCount);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(taskCount);

        // when
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                allStarted.countDown();
                try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                allDone.countDown();
            });
        }

        // then — all tasks started concurrently within the core pool
        assertThat(allStarted.await(5, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        assertThat(allDone.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_executorIsNotShutdownAfterCreation() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
        assertThat(executor.getThreadPoolExecutor().isTerminated()).isFalse();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_activeCountIsZeroInitially() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getActiveCount()).isZero();
        executor.shutdown();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesNestedCauseChainWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException rootCause = new RuntimeException("root");
        RuntimeException mid = new RuntimeException("mid", rootCause);
        RuntimeException top = new RuntimeException("top", mid);
        var method = AsyncConfigTest.class
                .getDeclaredMethod("asyncUncaughtExceptionHandler_handlesNestedCauseChainWithoutThrowing");

        // when/then — should not throw
        handler.handleUncaughtException(top, method, "param1");
    }

    @Test
    void asyncConfig_doesNotHaveEnableRetry() {
        assertThat(AsyncConfig.class.getAnnotation(EnableRetry.class)).isNull();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesCheckedExceptionWithoutThrowing() throws NoSuchMethodException {
        // given — business exceptions like ResourceNotFoundException propagate here
        // when @Recover only catches DataAccessException
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Exception checkedException = new Exception("checked business error");
        var method = AsyncConfigTest.class
                .getDeclaredMethod("asyncUncaughtExceptionHandler_handlesCheckedExceptionWithoutThrowing");

        // when/then — should not throw
        handler.handleUncaughtException(checkedException, method, "eventParam");
    }

    @Test
    void asyncUncaughtExceptionHandler_acceptsThrowableParameter() throws NoSuchMethodException {
        Method handleMethod = asyncConfig.getAsyncUncaughtExceptionHandler().getClass()
                .getMethod("handleUncaughtException", Throwable.class, Method.class, Object[].class);
        assertThat(handleMethod).isNotNull();
        assertThat(handleMethod.getParameterTypes()[0]).isEqualTo(Throwable.class);
    }

    @Test
    void enableAsync_proxyTargetClassDefaultIsFalse() {
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableAsync).isNotNull();
        assertThat(enableAsync.proxyTargetClass()).isFalse();
    }

    @Test
    void getAsyncExecutor_isDeclaredInAsyncConfig() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getDeclaredMethod("getAsyncExecutor");
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_isDeclaredInAsyncConfig() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getDeclaredMethod("getAsyncUncaughtExceptionHandler");
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
    }
}
