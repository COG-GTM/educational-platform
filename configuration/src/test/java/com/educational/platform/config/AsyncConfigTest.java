package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
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

    @Test
    void asyncUncaughtExceptionHandler_withNullParamValues_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException("test error");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, (Object) null))
                .doesNotThrowAnyException();
    }

    @Test
    void getAsyncExecutor_isInitializedAndReady() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getThreadPoolExecutor()).isNotNull();
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
    }

    @Test
    void getAsyncExecutor_hasCorrectQueueCapacity() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getQueueCapacity()).isEqualTo(100);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_returnsFreshInstanceEachCall() {
        // when
        AsyncUncaughtExceptionHandler handler1 = asyncConfig.getAsyncUncaughtExceptionHandler();
        AsyncUncaughtExceptionHandler handler2 = asyncConfig.getAsyncUncaughtExceptionHandler();

        // then
        assertThat(handler1).isNotSameAs(handler2);
    }

    @Test
    void class_hasConfigurationAnnotation() {
        // then
        assertThat(AsyncConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
    }

    @Test
    void class_implementsAsyncConfigurer() {
        // then
        assertThat(AsyncConfigurer.class).isAssignableFrom(AsyncConfig.class);
    }

    @Test
    void class_hasEnableAsyncAnnotationWithHighestPrecedenceOrder() {
        // when
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);

        // then
        assertThat(enableAsync).isNotNull();
        assertThat(enableAsync.order()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void class_hasEnableRetryAnnotationWithCorrectOrder() {
        // when
        EnableRetry enableRetry = AsyncConfig.class.getAnnotation(EnableRetry.class);

        // then
        assertThat(enableRetry).isNotNull();
        assertThat(enableRetry.order()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Test
    void enableAsyncOrderIsLowerThanEnableRetryOrder() {
        // when
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        EnableRetry enableRetry = AsyncConfig.class.getAnnotation(EnableRetry.class);

        // then
        assertThat(enableAsync.order()).isLessThan(enableRetry.order());
    }

    @Test
    void asyncUncaughtExceptionHandler_withNestedCause_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException cause = new RuntimeException("root cause");
        RuntimeException exception = new RuntimeException("wrapper", cause);

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "param1"))
                .doesNotThrowAnyException();
    }

}
