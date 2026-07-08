package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class AsyncConfigEnableAsyncIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableAsyncConfiguration.class,
                    LoggingAsyncUncaughtExceptionHandler.class, AsyncConfig.class);

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void asyncMethodExecutesOnIntegrationEventExecutorThread() {
        contextRunner.run(context -> {
            // given
            final AsyncSample sample = context.getBean(AsyncSample.class);
            final CompletableFuture<String> threadName = new CompletableFuture<>();

            // when
            sample.recordThreadName(threadName);

            // then
            assertThat(threadName.get(5, TimeUnit.SECONDS))
                    .startsWith(AsyncConfig.THREAD_NAME_PREFIX);
        });
    }

    @Test
    void exceptionFromVoidAsyncMethodIsRoutedToLoggingUncaughtExceptionHandler() {
        contextRunner.run(context -> {
            // given
            final AsyncSample sample = context.getBean(AsyncSample.class);
            final CountDownLatch invoked = new CountDownLatch(1);

            // when
            sample.explode(invoked, "course-42");

            // then
            assertThat(invoked.await(5, TimeUnit.SECONDS)).isTrue();
            awaitUntil(() -> !listAppender.list.isEmpty());
            final ILoggingEvent event = listAppender.list.get(0);
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .contains(AsyncSample.class.getName())
                    .contains("explode")
                    .contains("course-42]");
            assertThat(event.getThrowableProxy().getMessage()).isEqualTo("async boom");
        });
    }

    private static void awaitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within timeout");
            }
            Thread.sleep(50);
        }
    }

    @Configuration
    @EnableAsync
    static class EnableAsyncConfiguration {

        @Bean
        AsyncSample asyncSample() {
            return new AsyncSample();
        }
    }

    static class AsyncSample {

        @Async
        public void recordThreadName(CompletableFuture<String> threadName) {
            threadName.complete(Thread.currentThread().getName());
        }

        @Async
        public void explode(CountDownLatch invoked, String courseId) {
            invoked.countDown();
            throw new IllegalStateException("async boom");
        }
    }
}
