package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that AsyncConfig's exception handler correctly receives exceptions
 * that propagate past @Recover (i.e., non-retryable business exceptions).
 */
@SpringJUnitConfig(AsyncUncaughtExceptionHandlerIntegrationTest.TestConfig.class)
class AsyncUncaughtExceptionHandlerIntegrationTest {

    @Autowired
    private AsyncService asyncService;

    @Autowired
    private TestExceptionCaptor exceptionCaptor;

    @Test
    void asyncMethod_throwsException_uncaughtHandlerReceivesIt() throws Exception {
        // when
        asyncService.doWork("test-param");

        // then
        CapturedAsyncException captured = exceptionCaptor.poll(5, TimeUnit.SECONDS);
        assertThat(captured).isNotNull();
        assertThat(captured.exception())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("business error from async");
    }

    @Test
    void asyncMethod_uncaughtHandlerReceivesMethodName() throws Exception {
        // when
        asyncService.doWork("param-for-method-name");

        // then
        CapturedAsyncException captured = exceptionCaptor.poll(5, TimeUnit.SECONDS);
        assertThat(captured).isNotNull();
        assertThat(captured.method().getName()).isEqualTo("doWork");
    }

    @Test
    void asyncMethod_uncaughtHandlerReceivesParams() throws Exception {
        // when
        asyncService.doWork("captured-param");

        // then
        CapturedAsyncException captured = exceptionCaptor.poll(5, TimeUnit.SECONDS);
        assertThat(captured).isNotNull();
        assertThat(captured.params()).containsExactly("captured-param");
    }

    record CapturedAsyncException(Throwable exception, Method method, Object[] params) {}

    @Configuration
    @EnableAsync
    static class TestConfig implements AsyncConfigurer {

        @Bean
        TestExceptionCaptor testExceptionCaptor() {
            return new TestExceptionCaptor();
        }

        @Bean
        AsyncService asyncService() {
            return new AsyncService();
        }

        @Override
        public Executor getAsyncExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(2);
            executor.setThreadNamePrefix("test-async-");
            executor.initialize();
            return executor;
        }

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return (ex, method, params) -> testExceptionCaptor().offer(
                    new CapturedAsyncException(ex, method, params));
        }
    }

    static class AsyncService {

        @org.springframework.scheduling.annotation.Async
        public void doWork(String param) {
            throw new RuntimeException("business error from async");
        }
    }

    static class TestExceptionCaptor {

        private final BlockingQueue<CapturedAsyncException> queue = new LinkedBlockingQueue<>();

        void offer(CapturedAsyncException captured) {
            queue.offer(captured);
        }

        CapturedAsyncException poll(long timeout, TimeUnit unit) throws InterruptedException {
            return queue.poll(timeout, unit);
        }
    }
}
