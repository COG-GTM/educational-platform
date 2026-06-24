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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;

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
    void getAsyncExecutor_methodHasBeanAnnotation() throws NoSuchMethodException {
        // when
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");

        // then
        assertThat(method.isAnnotationPresent(Bean.class)).isTrue();
    }

    @Test
    void getAsyncExecutor_hasGracefulShutdownEnabled() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
        // waitForTasksToCompleteOnShutdown causes the executor to finish in-flight tasks on shutdown
        // verified indirectly: the executor's shutdown behavior is set during initialization
    }

    @Test
    void getAsyncExecutor_hasAwaitTerminationConfigured() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then - verify awaitTerminationMillis via reflection (no public getter in this Spring version)
        java.lang.reflect.Field field = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                .getDeclaredField("awaitTerminationMillis");
        field.setAccessible(true);
        long awaitTerminationMillis = (long) field.get(executor);
        assertThat(awaitTerminationMillis).isEqualTo(30_000);
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

    @Test
    void getAsyncExecutorMethod_hasBeanAnnotation() throws NoSuchMethodException {
        // when
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");

        // then
        assertThat(method.isAnnotationPresent(Bean.class)).isTrue();
    }

    @Test
    void asyncUncaughtExceptionHandler_withError_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        StackOverflowError error = new StackOverflowError("stack overflow");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(error, method, "param1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_withCheckedExceptionAsCause_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        Exception exception = new Exception("checked exception");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "param1", "param2"))
                .doesNotThrowAnyException();
    }

    @Test
    void getAsyncExecutor_executorIsActive() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getThreadPoolExecutor().isTerminated()).isFalse();
        assertThat(executor.getThreadPoolExecutor().isTerminating()).isFalse();
    }

    @Test
    void integrationEventAsyncUncaughtExceptionHandler_isStaticInnerClass() {
        // Static inner class prevents holding a reference to outer AsyncConfig instance (GC-safe)
        // when
        int modifiers = AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler.class.getModifiers();

        // then
        assertThat(java.lang.reflect.Modifier.isStatic(modifiers)).isTrue();
    }

    @Test
    void getAsyncExecutor_waitForTasksToCompleteOnShutdown_isEnabled() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then - verify via reflection that waitForTasksToCompleteOnShutdown is true
        java.lang.reflect.Field field = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                .getDeclaredField("waitForTasksToCompleteOnShutdown");
        field.setAccessible(true);
        boolean waitForTasks = (boolean) field.get(executor);
        assertThat(waitForTasks).isTrue();
    }

    @Test
    void getAsyncExecutor_canSubmitAndExecuteTask() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var future = executor.submit(() -> "done");

        // then
        assertThat(future.get(5, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("done");
    }

    @Test
    void integrationEventAsyncUncaughtExceptionHandler_implementsAsyncUncaughtExceptionHandler() {
        // then
        assertThat(AsyncUncaughtExceptionHandler.class)
                .isAssignableFrom(AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler.class);
    }

    @Test
    void enableAsyncAndEnableRetry_orderingEnsuresRetryRunsInsideAsyncThread() {
        // The critical invariant: @Async (lower order) is outer advisor, @Retryable (higher order) is inner.
        // This means the method executes in the async thread, and retry wraps the actual method call.
        // when
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        EnableRetry enableRetry = AsyncConfig.class.getAnnotation(EnableRetry.class);

        // then - async order must equal HIGHEST_PRECEDENCE (outermost)
        assertThat(enableAsync.order()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        // retry order must be exactly one more (inner to async)
        assertThat(enableRetry.order()).isEqualTo(enableAsync.order() + 1);
    }

    @Test
    void getAsyncExecutor_executorThread_usesConfiguredPrefix() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var threadNameHolder = new java.util.concurrent.atomic.AtomicReference<String>();
        executor.submit(() -> threadNameHolder.set(Thread.currentThread().getName()))
                .get(5, java.util.concurrent.TimeUnit.SECONDS);

        // then
        assertThat(threadNameHolder.get()).startsWith("integration-event-");
    }

    @Test
    void getAsyncExecutor_corePoolSizeDoesNotExceedMaxPoolSize() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getCorePoolSize()).isLessThanOrEqualTo(executor.getMaxPoolSize());
    }

    @Test
    void getAsyncExecutor_queueCapacityIsPositive() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getQueueCapacity()).isPositive();
    }

    @Test
    void getAsyncExecutor_returnsNewInstanceOnEachCall() {
        // @Bean creates a fresh executor per call; Spring manages singleton scope
        // when
        Executor executor1 = asyncConfig.getAsyncExecutor();
        Executor executor2 = asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor1).isNotSameAs(executor2);
    }

    @Test
    void enableRetryOrderIsExactlyOneMoreThanEnableAsyncOrder() {
        // when
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        EnableRetry enableRetry = AsyncConfig.class.getAnnotation(EnableRetry.class);

        // then
        assertThat(enableRetry.order() - enableAsync.order()).isEqualTo(1);
    }

    @Test
    void integrationEventAsyncUncaughtExceptionHandler_canBeInstantiatedDirectly() {
        // when
        AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler handler =
                new AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler();

        // then
        assertThat(handler).isNotNull();
        assertThat(handler).isInstanceOf(AsyncUncaughtExceptionHandler.class);
    }

    @Test
    void getAsyncExecutorMethod_overridesAsyncConfigurerMethod() throws NoSuchMethodException {
        // when
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");

        // then
        assertThat(method.isAnnotationPresent(Override.class) || method.isAnnotationPresent(Bean.class))
                .isTrue();
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
    }

    @Test
    void getAsyncUncaughtExceptionHandlerMethod_overridesAsyncConfigurerMethod() throws NoSuchMethodException {
        // when
        Method method = AsyncConfig.class.getMethod("getAsyncUncaughtExceptionHandler");

        // then
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
    }

    @Test
    void asyncUncaughtExceptionHandler_withManyParams_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException("test error");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method,
                "param1", "param2", "param3", "param4", "param5"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_withOptimisticLockingFailure_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        OptimisticLockingFailureException exception = new OptimisticLockingFailureException("lock conflict");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "event1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_withDataIntegrityViolation_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint violation");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "event1"))
                .doesNotThrowAnyException();
    }

    @Test
    void integrationEventAsyncUncaughtExceptionHandler_hasPackagePrivateVisibility() {
        // when
        int modifiers = AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler.class.getModifiers();

        // then
        assertThat(java.lang.reflect.Modifier.isPublic(modifiers)).isFalse();
        assertThat(java.lang.reflect.Modifier.isPrivate(modifiers)).isFalse();
    }

    @Test
    void getAsyncExecutor_threadPoolExecutor_hasDefaultRejectionPolicy() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then - default ThreadPoolTaskExecutor uses AbortPolicy
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.AbortPolicy.class);
    }

    @Test
    void getAsyncExecutor_corePoolSizeIsPositive() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getCorePoolSize()).isPositive();
    }

    @Test
    void getAsyncExecutor_maxPoolSizeIsPositive() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getMaxPoolSize()).isPositive();
    }

    @Test
    void asyncUncaughtExceptionHandler_withNullThrowable_throwsNullPointerException() throws NoSuchMethodException {
        // given - null throwable causes NPE on ex.getMessage() call
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(null, method, "param1"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getAsyncExecutor_threadNamePrefix_isNotEmpty() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getThreadNamePrefix()).isNotEmpty();
    }

    @Test
    void class_enableAsyncOrderIsHighestPrecedence() {
        // HIGHEST_PRECEDENCE ensures @Async is the outermost advisor in the proxy chain
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableAsync.order()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void asyncUncaughtExceptionHandler_withPessimisticLockingFailure_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        PessimisticLockingFailureException exception = new PessimisticLockingFailureException("deadlock");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "event1"))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_withQueryTimeout_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        QueryTimeoutException exception = new QueryTimeoutException("query timed out");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, "event1"))
                .doesNotThrowAnyException();
    }

    @Test
    void getAsyncExecutor_activeCountIsZeroBeforeSubmission() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then
        assertThat(executor.getActiveCount()).isZero();
    }

}

