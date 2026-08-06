package com.educational.platform.liquibase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;

@Configuration
public class LiquibaseLockConfiguration {

    @Bean
    @ConditionalOnProperty(value = "platform.liquibase.release-stale-locks", havingValue = "true", matchIfMissing = true)
    public static StaleLiquibaseLockReleaser staleLiquibaseLockReleaser(Environment environment) {
        Duration timeout = environment.getProperty("platform.liquibase.stale-lock-timeout", Duration.class, Duration.ofMinutes(5));
        return new StaleLiquibaseLockReleaser(timeout);
    }
}
