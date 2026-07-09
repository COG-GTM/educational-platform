package com.educational.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

class SchedulingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class, SampleConfiguration.class);

    @Test
    void schedulingConfigIsSpringConfigurationWithSchedulingEnabled() {
        // then
        assertThat(SchedulingConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
        assertThat(SchedulingConfig.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
    }

    @Test
    void schedulingConfigRegistersScheduledMethodsAsTasks() {
        contextRunner.run(context -> {
            // then
            final ScheduledAnnotationBeanPostProcessor processor = context.getBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(processor.getScheduledTasks())
                    .anySatisfy(task -> assertThat(task.toString()).contains(ScheduledSample.class.getName()));
        });
    }

    @Configuration
    static class SampleConfiguration {

        @Bean
        ScheduledSample scheduledSample() {
            return new ScheduledSample();
        }
    }

    static class ScheduledSample {

        @Scheduled(fixedDelay = 60_000L)
        public void poll() {
        }
    }
}
