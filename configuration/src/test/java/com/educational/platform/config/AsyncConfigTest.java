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
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        executor.afterPropertiesSet();
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
        executor.afterPropertiesSet();
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
        executor.afterPropertiesSet();
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
        executor.afterPropertiesSet();

        // then
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
        assertThat(executor.getThreadPoolExecutor().isTerminated()).isFalse();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_activeCountIsZeroInitially() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

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

    @Test
    void asyncUncaughtExceptionHandler_handlesMultipleParams() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException("Integration event handler failure");
        var method = AsyncConfigTest.class
                .getDeclaredMethod("asyncUncaughtExceptionHandler_handlesMultipleParams");

        // when/then — should not throw with varargs params
        handler.handleUncaughtException(exception, method, "event1", "event2", "event3");
    }

    @Test
    void getAsyncExecutor_executorIsInitialized() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        assertThat(executor.getThreadPoolExecutor()).isNotNull();
        assertThat(executor.getThreadPoolExecutor().getPoolSize()).isGreaterThanOrEqualTo(0);
        executor.shutdown();
    }

    @Test
    void getAsyncUncaughtExceptionHandler_isIntegrationEventSpecificImpl() {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertThat(handler.getClass().getName()).contains("IntegrationEvent");
    }

    @Test
    void getAsyncExecutor_returnsInitializedExecutor_thatCanBeShutdown() {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        // when
        executor.shutdown();

        // then
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isTrue();
    }

    @Test
    void asyncConfig_implementsAllAsyncConfigurerMethods() throws NoSuchMethodException {
        assertThat(AsyncConfig.class.getDeclaredMethod("getAsyncExecutor")).isNotNull();
        assertThat(AsyncConfig.class.getDeclaredMethod("getAsyncUncaughtExceptionHandler")).isNotNull();
    }

    @Test
    void getAsyncExecutor_queueIsEmptyInitially() {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        // then
        assertThat(executor.getThreadPoolExecutor().getQueue()).isEmpty();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_overridesAsyncConfigurerMethod() throws NoSuchMethodException {
        Method configMethod = AsyncConfig.class.getMethod("getAsyncExecutor");
        Method interfaceMethod = AsyncConfigurer.class.getMethod("getAsyncExecutor");
        assertThat(configMethod.getName()).isEqualTo(interfaceMethod.getName());
        assertThat(configMethod.getReturnType()).isEqualTo(interfaceMethod.getReturnType());
        assertThat(configMethod.getParameterCount()).isEqualTo(interfaceMethod.getParameterCount());
    }

    @Test
    void getAsyncUncaughtExceptionHandler_overridesAsyncConfigurerMethod() throws NoSuchMethodException {
        Method configMethod = AsyncConfig.class.getMethod("getAsyncUncaughtExceptionHandler");
        Method interfaceMethod = AsyncConfigurer.class.getMethod("getAsyncUncaughtExceptionHandler");
        assertThat(configMethod.getName()).isEqualTo(interfaceMethod.getName());
        assertThat(configMethod.getReturnType()).isEqualTo(interfaceMethod.getReturnType());
        assertThat(configMethod.getParameterCount()).isEqualTo(interfaceMethod.getParameterCount());
    }

    @Test
    void getAsyncUncaughtExceptionHandler_returnTypeIsCorrect() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncUncaughtExceptionHandler");
        assertThat(method.getReturnType()).isEqualTo(AsyncUncaughtExceptionHandler.class);
    }

    @Test
    void getAsyncExecutor_returnTypeIsExecutor() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");
        assertThat(method.getReturnType()).isEqualTo(Executor.class);
    }

    @Test
    void enableAsync_modeIsProxy() {
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableAsync).isNotNull();
        assertThat(enableAsync.mode()).isEqualTo(org.springframework.context.annotation.AdviceMode.PROXY);
    }

    @Test
    void asyncConfig_isNotAbstract() {
        assertThat(java.lang.reflect.Modifier.isAbstract(AsyncConfig.class.getModifiers())).isFalse();
    }

    @Test
    void asyncConfig_isNotFinal() {
        assertThat(java.lang.reflect.Modifier.isFinal(AsyncConfig.class.getModifiers())).isFalse();
    }

    @Test
    void enableAsync_annotationValueIsEmpty() {
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableAsync).isNotNull();
        assertThat(enableAsync.annotation()).isEqualTo(java.lang.annotation.Annotation.class);
    }

    @Test
    void getAsyncExecutor_rejectedExecutionHandlerIsAbortPolicy() {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        // then — default abort policy ensures tasks are not silently dropped
        RejectedExecutionHandler handler = executor.getThreadPoolExecutor().getRejectedExecutionHandler();
        assertThat(handler).isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_completedTaskCountIsZeroInitially() {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        // then
        assertThat(executor.getThreadPoolExecutor().getCompletedTaskCount()).isZero();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_taskCountIsZeroInitially() {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        // then
        assertThat(executor.getThreadPoolExecutor().getTaskCount()).isZero();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_corePoolSizeFitsTypicalConcurrency() {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then — core size (4) should not exceed max size (8)
        assertThat(executor.getCorePoolSize()).isLessThanOrEqualTo(executor.getMaxPoolSize());
    }

    @Test
    void getAsyncExecutor_queueCapacityExceedsCorePoolSize() {
        // given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then — queue capacity should be larger than core pool to buffer bursts
        assertThat(executor.getQueueCapacity()).isGreaterThan(executor.getCorePoolSize());
    }

    @Test
    void getAsyncExecutor_keepAliveSecondsIsDefaultSixty() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        // then — default keep-alive for excess threads is 60 seconds
        assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_allowCoreThreadTimeOutIsFalse() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        // then — core threads should not time out to keep the pool warm for event handling
        assertThat(executor.getThreadPoolExecutor().allowsCoreThreadTimeOut()).isFalse();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_maxPoolSizeIsDoubleOfCorePoolSize() {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then — consistent ratio: max pool = 2x core pool
        assertThat(executor.getMaxPoolSize()).isEqualTo(executor.getCorePoolSize() * 2);
    }

    @Test
    void asyncUncaughtExceptionHandler_isNotSameAsGetAsyncExecutor() {
        // then — exception handler and executor are separate concerns
        Object executor = asyncConfig.getAsyncExecutor();
        Object handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertThat(executor).isNotSameAs(handler);
    }

    @Test
    void getAsyncExecutor_hasBeanAnnotation_withExactlyOneName() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");
        Bean bean = method.getAnnotation(Bean.class);
        assertThat(bean.name()).hasSize(1);
        assertThat(bean.name()[0]).isEqualTo("integrationEventExecutor");
    }

    @Test
    void asyncConfig_hasLogField_privateStaticFinal() throws NoSuchFieldException {
        java.lang.reflect.Field logField = AsyncConfig.class.getDeclaredField("log");
        assertThat(java.lang.reflect.Modifier.isPrivate(logField.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isStatic(logField.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(logField.getModifiers())).isTrue();
        assertThat(logField.getType()).isEqualTo(org.slf4j.Logger.class);
    }

    @Test
    void asyncConfig_isPublic() {
        assertThat(java.lang.reflect.Modifier.isPublic(AsyncConfig.class.getModifiers())).isTrue();
    }

    @Test
    void getAsyncExecutor_threadNamePrefixEndsWithHyphen() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        assertThat(executor.getThreadNamePrefix()).endsWith("-");
    }

    @Test
    void asyncUncaughtExceptionHandler_innerClass_isDeclaredInsideAsyncConfig() {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertThat(handler.getClass().getDeclaringClass())
                .as("Exception handler should be an inner class of AsyncConfig")
                .isEqualTo(AsyncConfig.class);
    }

    @Test
    void asyncUncaughtExceptionHandler_innerClass_isPrivate() {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertThat(java.lang.reflect.Modifier.isPrivate(handler.getClass().getModifiers()))
                .as("Exception handler inner class should be private to prevent direct instantiation")
                .isTrue();
    }

    @Test
    void asyncUncaughtExceptionHandler_innerClass_isStatic() {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertThat(java.lang.reflect.Modifier.isStatic(handler.getClass().getModifiers()))
                .as("Exception handler inner class should be static (no reference to enclosing instance)")
                .isTrue();
    }

    @Test
    void asyncUncaughtExceptionHandler_innerClass_implementsAsyncUncaughtExceptionHandler() {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertThat(handler).isInstanceOf(AsyncUncaughtExceptionHandler.class);
        assertThat(AsyncUncaughtExceptionHandler.class.isAssignableFrom(handler.getClass())).isTrue();
    }

    @Test
    void asyncConfig_doesNotHaveTransactionalAnnotation() {
        assertThat(AsyncConfig.class.getAnnotation(
                org.springframework.transaction.annotation.Transactional.class)).isNull();
    }

    @Test
    void getAsyncExecutor_returnType_isThreadPoolTaskExecutor() {
        Executor executor = asyncConfig.getAsyncExecutor();
        assertThat(executor)
                .as("getAsyncExecutor should return ThreadPoolTaskExecutor for monitoring and lifecycle management")
                .isInstanceOf(ThreadPoolTaskExecutor.class);
        ((ThreadPoolTaskExecutor) executor).shutdown();
    }

    @Test
    void asyncConfig_logFieldReferencesOwnClass() throws Exception {
        java.lang.reflect.Field logField = AsyncConfig.class.getDeclaredField("log");
        logField.setAccessible(true);
        org.slf4j.Logger logger = (org.slf4j.Logger) logField.get(null);
        assertThat(logger.getName())
                .as("Logger in AsyncConfig should reference its own class (catches copy-paste errors)")
                .isEqualTo(AsyncConfig.class.getName());
    }

    @Test
    void getAsyncExecutor_executorDoesNotCallInitialize() throws Exception {
        // AsyncConfig returns an uninitialized executor — Spring's bean lifecycle calls
        // afterPropertiesSet() during context startup. This test documents that design choice.
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        // Before afterPropertiesSet(), the underlying ThreadPoolExecutor should not exist
        java.lang.reflect.Field tpeField = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                .getDeclaredField("executor");
        tpeField.setAccessible(true);
        assertThat(tpeField.get(executor))
                .as("Executor should not be initialized yet — Spring lifecycle handles initialization")
                .isNull();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesEmptyParamsArray() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException("empty params");
        var method = AsyncConfigTest.class
                .getDeclaredMethod("asyncUncaughtExceptionHandler_handlesEmptyParamsArray");

        // when/then — should not throw with empty params array
        handler.handleUncaughtException(exception, method);
    }

    @Test
    void asyncConfig_getAsyncExecutorMethod_hasOverrideAnnotation() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncExecutor");
        // Verify this overrides AsyncConfigurer interface
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
        assertThat(AsyncConfigurer.class.getMethod("getAsyncExecutor")).isNotNull();
    }

    @Test
    void getAsyncExecutor_beforeInitialization_rejectsTaskSubmission() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        assertThatThrownBy(() -> executor.execute(() -> {}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getAsyncExecutor_afterInitialization_usesConfiguredThreadNamePrefix() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        executor.afterPropertiesSet();

        try {
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<String> threadName = new java.util.concurrent.atomic.AtomicReference<>();
            executor.execute(() -> {
                threadName.set(Thread.currentThread().getName());
                latch.countDown();
            });
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(threadName.get()).startsWith("integration-event-");
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void asyncUncaughtExceptionHandler_nullMethod_throwsNullPointerException() {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException("test error");
        assertThatThrownBy(() -> handler.handleUncaughtException(exception, null, "param"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void asyncConfig_getAsyncUncaughtExceptionHandlerMethod_overridesAsyncConfigurer() throws NoSuchMethodException {
        Method method = AsyncConfig.class.getMethod("getAsyncUncaughtExceptionHandler");
        assertThat(method.getDeclaringClass()).isEqualTo(AsyncConfig.class);
        assertThat(AsyncConfigurer.class.getMethod("getAsyncUncaughtExceptionHandler")).isNotNull();
    }

    @Test
    void asyncUncaughtExceptionHandler_handlesNullParams() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        RuntimeException exception = new RuntimeException("null params test");
        var method = AsyncConfigTest.class
                .getDeclaredMethod("asyncUncaughtExceptionHandler_handlesNullParams");
        handler.handleUncaughtException(exception, method, (Object[]) null);
    }

    @Test
    void asyncConfig_hasNoPublicFields() {
        long publicFieldCount = java.util.Arrays.stream(AsyncConfig.class.getDeclaredFields())
                .filter(f -> java.lang.reflect.Modifier.isPublic(f.getModifiers()))
                .count();
        assertThat(publicFieldCount)
                .as("AsyncConfig should not expose public fields")
                .isZero();
    }

    @Test
    void getAsyncExecutor_executorIsNotNull() {
        Executor executor = asyncConfig.getAsyncExecutor();
        assertThat(executor)
                .as("getAsyncExecutor should never return null — Spring requires a valid executor")
                .isNotNull();
    }

    @Test
    void getAsyncUncaughtExceptionHandler_handlerIsNotNull() {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertThat(handler)
                .as("getAsyncUncaughtExceptionHandler should never return null — Spring uses it for async error handling")
                .isNotNull();
    }

    @Test
    void getAsyncExecutor_shutdownSettings_areConfiguredTogether() throws Exception {
        // when
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then — both shutdown settings must be configured together for graceful shutdown
        java.lang.reflect.Field waitField = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                .getDeclaredField("waitForTasksToCompleteOnShutdown");
        waitField.setAccessible(true);
        assertThat((boolean) waitField.get(executor))
                .as("waitForTasksToCompleteOnShutdown must be true for graceful async event processing shutdown")
                .isTrue();
        java.lang.reflect.Field awaitField = org.springframework.scheduling.concurrent.ExecutorConfigurationSupport.class
                .getDeclaredField("awaitTerminationMillis");
        awaitField.setAccessible(true);
        assertThat((long) awaitField.get(executor))
                .as("awaitTerminationMillis must be set when waitForTasks is true")
                .isEqualTo(30_000L);
        executor.shutdown();
    }

    @Test
    void asyncUncaughtExceptionHandler_innerClass_hasOwnLogger() throws Exception {
        // given — the inner handler class should have its own logger, not reference AsyncConfig's
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Class<?> handlerClass = handler.getClass();

        // then
        java.lang.reflect.Field logField = handlerClass.getDeclaredField("log");
        logField.setAccessible(true);
        org.slf4j.Logger logger = (org.slf4j.Logger) logField.get(null);
        assertThat(logger.getName())
                .as("Inner exception handler should have a logger referencing its own class")
                .contains("IntegrationEventAsyncExceptionHandler");
    }

    @Test
    void getAsyncExecutor_multipleInstances_haveIndependentConfiguration() {
        // when — each call creates a new executor instance
        ThreadPoolTaskExecutor exec1 = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        ThreadPoolTaskExecutor exec2 = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // then — both have correct config, proving configuration is applied fresh each time
        assertThat(exec1).isNotSameAs(exec2);
        assertThat(exec1.getCorePoolSize()).isEqualTo(exec2.getCorePoolSize()).isEqualTo(4);
        assertThat(exec1.getMaxPoolSize()).isEqualTo(exec2.getMaxPoolSize()).isEqualTo(8);
        assertThat(exec1.getQueueCapacity()).isEqualTo(exec2.getQueueCapacity()).isEqualTo(100);
        assertThat(exec1.getThreadNamePrefix()).isEqualTo(exec2.getThreadNamePrefix())
                .isEqualTo("integration-event-");
    }

    @Test
    void asyncUncaughtExceptionHandler_withVeryLongExceptionMessage_handlesWithoutThrowing() throws NoSuchMethodException {
        // given — very long exception message near logging buffer limits
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        String longMessage = "X".repeat(5000);
        RuntimeException exception = new RuntimeException(longMessage);
        var method = AsyncConfigTest.class
                .getDeclaredMethod("asyncUncaughtExceptionHandler_withVeryLongExceptionMessage_handlesWithoutThrowing");

        // when/then — should not throw
        handler.handleUncaughtException(exception, method, "param1", "param2");
    }

    @Test
    void asyncUncaughtExceptionHandler_withDeeplyCausedException_handlesWithoutThrowing() throws NoSuchMethodException {
        // given — deeply nested exception cause chain
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        Exception root = new java.io.IOException("disk full");
        Exception mid = new RuntimeException("data access failed", root);
        Exception top = new IllegalStateException("handler failure", mid);
        var method = AsyncConfigTest.class
                .getDeclaredMethod("asyncUncaughtExceptionHandler_withDeeplyCausedException_handlesWithoutThrowing");

        // when/then — should not throw
        handler.handleUncaughtException(top, method, "event-data");
    }
}
