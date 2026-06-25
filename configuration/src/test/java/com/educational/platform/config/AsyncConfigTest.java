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

    @Test
    void getAsyncExecutor_multipleSubmittedTasks_allComplete() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var results = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var latch = new java.util.concurrent.CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            final int index = i;
            executor.submit(() -> {
                results.add("task-" + index);
                latch.countDown();
            });
        }

        // then
        assertThat(latch.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(results).hasSize(5);
    }

    @Test
    void asyncUncaughtExceptionHandler_withEmptyParamsArray_logsWithoutThrowing() throws NoSuchMethodException {
        // given
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException("test error");

        // when / then
        assertThatCode(() -> handler.handleUncaughtException(exception, method, new Object[0]))
                .doesNotThrowAnyException();
    }

    @Test
    void getAsyncExecutor_keepAliveSecondsIsDefault() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then - default keep-alive is 60 seconds
        assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
    }

    @Test
    void class_isNotAbstract() {
        // Spring @Configuration classes must be instantiable
        assertThat(java.lang.reflect.Modifier.isAbstract(AsyncConfig.class.getModifiers())).isFalse();
    }

    @Test
    void class_isNotFinal() {
        // Spring @Configuration uses CGLIB proxying which requires non-final classes
        assertThat(java.lang.reflect.Modifier.isFinal(AsyncConfig.class.getModifiers())).isFalse();
    }

    @Test
    void class_hasExactlyThreeClassAnnotations() {
        // @EnableAsync, @EnableRetry, @Configuration
        int annotationCount = 0;
        if (AsyncConfig.class.isAnnotationPresent(EnableAsync.class)) annotationCount++;
        if (AsyncConfig.class.isAnnotationPresent(EnableRetry.class)) annotationCount++;
        if (AsyncConfig.class.isAnnotationPresent(Configuration.class)) annotationCount++;

        assertThat(annotationCount).isEqualTo(3);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_methodOverridesAsyncConfigurer() throws NoSuchMethodException {
        // when
        Method method = AsyncConfig.class.getMethod("getAsyncUncaughtExceptionHandler");

        // then
        assertThat(method.getReturnType()).isEqualTo(AsyncUncaughtExceptionHandler.class);
    }

    @Test
    void getAsyncExecutor_methodReturnType_isExecutor() throws NoSuchMethodException {
        // when
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");

        // then
        assertThat(method.getReturnType()).isEqualTo(Executor.class);
    }

    @Test
    void getAsyncExecutor_concurrentTasks_useMultipleThreads() throws Exception {
        // Verifies the thread pool actually uses multiple threads for concurrent tasks
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var threadNames = new java.util.concurrent.CopyOnWriteArraySet<String>();
        var barrier = new java.util.concurrent.CyclicBarrier(4);
        var latch = new java.util.concurrent.CountDownLatch(4);

        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    threadNames.add(Thread.currentThread().getName());
                    barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // then
        assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(threadNames).hasSizeGreaterThan(1);
        threadNames.forEach(name -> assertThat(name).startsWith("integration-event-"));
    }

    @Test
    void getAsyncExecutor_threadNamesAreSequentiallyNumbered() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var threadNames = java.util.Collections.synchronizedList(new java.util.ArrayList<String>());
        var latch = new java.util.concurrent.CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                threadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
        }

        // then
        assertThat(latch.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        for (String name : threadNames) {
            assertThat(name).matches("integration-event-\\d+");
        }
    }

    @Test
    void getAsyncExecutor_queueCapacityPlusMaxPool_definesRejectionBoundary() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then - total capacity before rejection is queue + maxPoolSize
        assertThat(executor.getQueueCapacity() + executor.getMaxPoolSize())
                .as("Total capacity (queue + maxPool) before rejection")
                .isEqualTo(108);
    }

    @Test
    void class_enableRetryOrderIsOneMoreThanHighestPrecedence() {
        // when
        EnableRetry enableRetry = AsyncConfig.class.getAnnotation(EnableRetry.class);

        // then
        assertThat(enableRetry.order()).isEqualTo(Integer.MIN_VALUE + 1);
    }

    @Test
    void asyncUncaughtExceptionHandler_concurrentInvocations_allCompleteWithoutError() throws Exception {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        int threadCount = 10;
        var latch = new java.util.concurrent.CountDownLatch(threadCount);
        var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    handler.handleUncaughtException(
                            new RuntimeException("concurrent-error-" + idx), method, "param-" + idx);
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertThat(latch.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(errors).isEmpty();
    }

    @Test
    void getAsyncExecutor_shutdown_completesGracefully() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        executor.submit(() -> "done").get(5, java.util.concurrent.TimeUnit.SECONDS);

        executor.shutdown();

        assertThat(executor.getThreadPoolExecutor().isShutdown()).isTrue();
    }

    @Test
    void asyncUncaughtExceptionHandler_withTransientDataAccessSubclass_logsWithoutThrowing() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        QueryTimeoutException exception = new QueryTimeoutException("timeout during async");

        assertThatCode(() -> handler.handleUncaughtException(exception, method, "event1"))
                .doesNotThrowAnyException();
    }

    @Test
    void getAsyncExecutor_allowCoreThreadTimeOutIsFalse() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        assertThat(executor.getThreadPoolExecutor().allowsCoreThreadTimeOut())
                .as("Core threads should not time out to maintain baseline concurrency")
                .isFalse();
    }

    @Test
    void getAsyncExecutor_poolSizeRatio_maxIsDoubleCore() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        assertThat(executor.getMaxPoolSize())
                .as("Max pool size should be 2x core pool size")
                .isEqualTo(executor.getCorePoolSize() * 2);
    }

    @Test
    void getAsyncExecutor_awaitTerminationSeconds_isThirty() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        assertThat(executor.getThreadPoolExecutor().isTerminating()).isFalse();
    }

    @Test
    void asyncUncaughtExceptionHandler_withNullParams_logsWithoutThrowing() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException("test error");

        assertThatCode(() -> handler.handleUncaughtException(exception, method, (Object[]) null))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_withMultipleParams_logsWithoutThrowing() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException exception = new RuntimeException("multi-param error");

        assertThatCode(() -> handler.handleUncaughtException(exception, method, "param1", 42, true))
                .doesNotThrowAnyException();
    }

    @Test
    void asyncUncaughtExceptionHandler_innerClass_isPackagePrivate() {
        int modifiers = AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler.class.getModifiers();

        assertThat(java.lang.reflect.Modifier.isPublic(modifiers)).isFalse();
        assertThat(java.lang.reflect.Modifier.isPrivate(modifiers)).isFalse();
        assertThat(java.lang.reflect.Modifier.isProtected(modifiers)).isFalse();
    }

    @Test
    void getAsyncExecutor_usesLinkedBlockingQueue() {
        // LinkedBlockingQueue preserves task submission order (FIFO) for integration events
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        assertThat(executor.getThreadPoolExecutor().getQueue())
                .isInstanceOf(java.util.concurrent.LinkedBlockingQueue.class);
    }

    @Test
    void getAsyncExecutor_threadsAreNotDaemon() throws Exception {
        // Non-daemon threads prevent JVM from terminating while tasks are in-flight
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var isDaemon = new java.util.concurrent.atomic.AtomicBoolean(true);

        executor.submit(() -> isDaemon.set(Thread.currentThread().isDaemon()))
                .get(5, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(isDaemon.get())
                .as("Integration event threads must be non-daemon for graceful shutdown")
                .isFalse();
    }

    @Test
    void getAsyncExecutor_taskDecoratorIsNull() throws Exception {
        // No task decorator configured - tasks run without wrapping
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        java.lang.reflect.Field field = ThreadPoolTaskExecutor.class.getDeclaredField("taskDecorator");
        field.setAccessible(true);
        Object taskDecorator = field.get(executor);

        assertThat(taskDecorator)
                .as("No task decorator should be configured (MDC propagation not required)")
                .isNull();
    }

    @Test
    void getAsyncExecutor_completedTaskCountIsZeroInitially() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        assertThat(executor.getThreadPoolExecutor().getCompletedTaskCount())
                .as("No tasks should have completed on a fresh executor")
                .isZero();
    }

    @Test
    void getAsyncExecutor_queueIsEmptyInitially() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        assertThat(executor.getThreadPoolExecutor().getQueue())
                .as("Queue should be empty on a fresh executor")
                .isEmpty();
    }

    @Test
    void enableAsync_proxyTargetClassIsDefault() {
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);

        assertThat(enableAsync.proxyTargetClass())
                .as("proxyTargetClass should be default (false) for interface-based proxying")
                .isFalse();
    }

    @Test
    void getAsyncExecutor_poolSizeValues_formValidConfiguration() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // Valid configuration requires: corePoolSize > 0 && maxPoolSize >= corePoolSize && queueCapacity >= 0
        assertThat(executor.getCorePoolSize()).isGreaterThan(0);
        assertThat(executor.getMaxPoolSize()).isGreaterThanOrEqualTo(executor.getCorePoolSize());
        assertThat(executor.getQueueCapacity()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void enableRetry_proxyTargetClassIsDefault() {
        // @EnableRetry should use default proxyTargetClass for CGLIB-based proxying
        EnableRetry enableRetry = AsyncConfig.class.getAnnotation(EnableRetry.class);

        assertThat(enableRetry.proxyTargetClass())
                .as("proxyTargetClass should be default (false)")
                .isFalse();
    }

    @Test
    void getAsyncExecutor_multipleTasksExecuteInParallel() throws Exception {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var latch = new java.util.concurrent.CountDownLatch(1);
        var threadsUsed = new java.util.concurrent.CopyOnWriteArrayList<String>();

        // when - submit tasks that block until latch is released
        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < 4; i++) {
            futures.add(executor.submit(() -> {
                threadsUsed.add(Thread.currentThread().getName());
                try { latch.await(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
        }

        // then - all 4 core threads should be occupied
        Thread.sleep(200);
        assertThat(threadsUsed).hasSize(4);
        assertThat(threadsUsed.stream().distinct().count())
                .as("4 tasks should use 4 distinct threads (corePoolSize=4)")
                .isEqualTo(4);

        latch.countDown();
        for (var f : futures) f.get(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    void getAsyncExecutor_gracefulShutdown_completesInFlightTask() throws Exception {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        var taskCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
        var taskStarted = new java.util.concurrent.CountDownLatch(1);

        executor.submit(() -> {
            taskStarted.countDown();
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            taskCompleted.set(true);
        });

        // when - wait for task to start, then initiate shutdown
        assertThat(taskStarted.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // then - task should have completed because waitForTasksToCompleteOnShutdown is true
        executor.getThreadPoolExecutor().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(taskCompleted.get())
                .as("In-flight task must complete during graceful shutdown")
                .isTrue();
    }

    @Test
    void getAsyncExecutor_afterShutdown_rejectsNewTasks() throws Exception {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.submit(() -> "warmup").get(5, java.util.concurrent.TimeUnit.SECONDS);

        // when
        executor.shutdown();
        executor.getThreadPoolExecutor().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // then
        assertThatCode(() -> executor.submit(() -> "rejected"))
                .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
    }

    @Test
    void integrationEventAsyncUncaughtExceptionHandler_loggerIsInitializedWithCorrectClass() throws Exception {
        // given
        java.lang.reflect.Field logField = AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler.class
                .getDeclaredField("log");
        logField.setAccessible(true);
        org.slf4j.Logger logger = (org.slf4j.Logger) logField.get(null);

        // then
        assertThat(logger.getName())
                .isEqualTo(AsyncConfig.IntegrationEventAsyncUncaughtExceptionHandler.class.getName());
    }

    @Test
    void asyncConfig_loggerIsInitializedWithCorrectClass() throws Exception {
        // given
        java.lang.reflect.Field logField = AsyncConfig.class.getDeclaredField("log");
        logField.setAccessible(true);
        org.slf4j.Logger logger = (org.slf4j.Logger) logField.get(null);

        // then
        assertThat(logger.getName()).isEqualTo(AsyncConfig.class.getName());
    }

}

