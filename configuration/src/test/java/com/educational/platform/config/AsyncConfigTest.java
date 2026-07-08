package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LoggingAsyncUncaughtExceptionHandler.class, AsyncConfig.class);

    @Test
    void integrationEventExecutorBeanIsPresentAndConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR);

            final ThreadPoolTaskExecutor executor =
                    context.getBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR, ThreadPoolTaskExecutor.class);

            assertThat(executor.getCorePoolSize()).isEqualTo(AsyncConfig.CORE_POOL_SIZE);
            assertThat(executor.getMaxPoolSize()).isEqualTo(AsyncConfig.MAX_POOL_SIZE);
            assertThat(executor.getQueueCapacity()).isEqualTo(AsyncConfig.QUEUE_CAPACITY);
            assertThat(executor.getThreadNamePrefix()).isEqualTo(AsyncConfig.THREAD_NAME_PREFIX);
        });
    }

    @Test
    void asyncConfigurerExposesExecutorAndUncaughtExceptionHandler() {
        contextRunner.run(context -> {
            final AsyncConfigurer asyncConfigurer = context.getBean(AsyncConfig.class);

            assertThat(asyncConfigurer.getAsyncExecutor())
                    .isSameAs(context.getBean(AsyncConfig.INTEGRATION_EVENT_EXECUTOR));
            assertThat(asyncConfigurer.getAsyncUncaughtExceptionHandler())
                    .isInstanceOf(LoggingAsyncUncaughtExceptionHandler.class);
        });
    }
}
