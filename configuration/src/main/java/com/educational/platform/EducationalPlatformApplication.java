package com.educational.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

// Async advice must be the outer proxy so retries run on the executor thread.
@EnableAsync(order = Ordered.LOWEST_PRECEDENCE - 1)
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE)
@SpringBootApplication
@PropertySource("application-security.properties")
public class EducationalPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducationalPlatformApplication.class, args);
    }

}

